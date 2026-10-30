"""Dựng lại file từ thư mục dữ liệu MinIO (định dạng xl-single, 1 ổ, không versioning) khi không chạy được
MinIO trên dữ liệu đó. Dùng một lần khi chuyển ảnh/video từ monolith sang.

Định dạng (đã kiểm chứng trên dữ liệu thật):
  <bucket>/<key>/xl.meta                    "XL2 " + u16 major + u16 minor + msgpack(bin metadata) + crc + inline data
  <bucket>/<key>/<dataDir>/part.N           [hash 32 byte][khối <= EcBSize] lặp lại (bitrot streaming)
File nhỏ không có dataDir: dữ liệu nằm trong phần inline của xl.meta, cũng bọc hash theo khối.

Chạy:  python extract_minio_xl.py <thư mục dữ liệu minio> <thư mục xuất>
Mỗi file xuất kèm <file>.content-type. Kích thước phải khớp metadata, sai là báo lỗi và bỏ qua.
"""
import os
import struct
import sys

HASH = 32


class Reader:
    def __init__(self, b, pos=0):
        self.b, self.p = b, pos

    def take(self, n):
        v = self.b[self.p:self.p + n]
        self.p += n
        return v

    def u(self, fmt):
        n = struct.calcsize(fmt)
        return struct.unpack('>' + fmt, self.take(n))[0]

    def obj(self):  # msgpack tối giản
        t = self.take(1)[0]
        if t <= 0x7f: return t
        if 0x80 <= t <= 0x8f: return self.map(t & 0x0f)
        if 0x90 <= t <= 0x9f: return [self.obj() for _ in range(t & 0x0f)]
        if 0xa0 <= t <= 0xbf: return self.take(t & 0x1f).decode('utf-8', 'replace')
        if t >= 0xe0: return t - 0x100
        return {
            0xc0: lambda: None, 0xc2: lambda: False, 0xc3: lambda: True,
            0xc4: lambda: self.take(self.u('B')), 0xc5: lambda: self.take(self.u('H')), 0xc6: lambda: self.take(self.u('I')),
            0xc7: lambda: self.ext(self.u('B')),
            0xc8: lambda: self.ext(self.u('H')), 0xc9: lambda: self.ext(self.u('I')),
            0xca: lambda: self.u('f'), 0xcb: lambda: self.u('d'),
            0xcc: lambda: self.u('B'), 0xcd: lambda: self.u('H'), 0xce: lambda: self.u('I'), 0xcf: lambda: self.u('Q'),
            0xd0: lambda: self.u('b'), 0xd1: lambda: self.u('h'), 0xd2: lambda: self.u('i'), 0xd3: lambda: self.u('q'),
            0xd4: lambda: self.ext(1), 0xd5: lambda: self.ext(2), 0xd6: lambda: self.ext(4), 0xd7: lambda: self.ext(8),
            0xd8: lambda: self.ext(16),
            0xd9: lambda: self.take(self.u('B')).decode('utf-8', 'replace'),
            0xda: lambda: self.take(self.u('H')).decode('utf-8', 'replace'),
            0xdb: lambda: self.take(self.u('I')).decode('utf-8', 'replace'),
            0xdc: lambda: [self.obj() for _ in range(self.u('H'))],
            0xdd: lambda: [self.obj() for _ in range(self.u('I'))],
            0xde: lambda: self.map(self.u('H')), 0xdf: lambda: self.map(self.u('I')),
        }[t]()

    def ext(self, n):
        self.u('b')
        return self.take(n)

    def map(self, n):
        return {self.obj(): self.obj() for _ in range(n)}


def unwrap(blob, block):
    """Bỏ hash 32 byte đứng trước mỗi khối."""
    out, p = bytearray(), 0
    while p < len(blob):
        chunk = blob[p + HASH:p + HASH + block]
        out += chunk
        p += HASH + block
    return bytes(out)


def parse(meta_path):
    b = open(meta_path, 'rb').read()
    assert b[:4] == b'XL2 ', 'không phải xl.meta v2'
    r = Reader(b, 8)
    header = r.obj()                      # bin chứa danh sách phiên bản
    try:
        r.obj()                           # crc
    except Exception:
        pass
    inline = {}
    if r.p < len(b) and b[r.p] == 1:      # inline data: byte phiên bản + map key -> bin
        r.p += 1
        inline = r.obj()
    h = Reader(header)
    h.obj(); h.obj()                      # header version, meta version
    count = h.obj()
    versions = []
    for _ in range(count):
        h.obj()                           # header từng version
        versions.append(Reader(h.obj()).obj())
    v = next(x for x in versions if isinstance(x, dict) and x.get('V2Obj'))
    return v['V2Obj'], inline


def main(src, dst):
    ok = bad = 0
    for bucket in sorted(os.listdir(src)):
        if bucket.startswith('.'):
            continue
        for root, _dirs, files in os.walk(os.path.join(src, bucket)):
            if 'xl.meta' not in files:
                continue
            key = os.path.relpath(root, os.path.join(src, bucket)).replace(os.sep, '/')
            try:
                obj, inline = parse(os.path.join(root, 'xl.meta'))
                block = obj['EcBSize']
                size = obj['Size']
                ddir = obj.get('DDir')
                ddir_name = None
                if isinstance(ddir, (bytes, bytearray)) and len(ddir) == 16:
                    hx = ddir.hex()
                    ddir_name = f'{hx[:8]}-{hx[8:12]}-{hx[12:16]}-{hx[16:20]}-{hx[20:]}'
                part_dir = os.path.join(root, ddir_name) if ddir_name else None
                if part_dir and os.path.isdir(part_dir):
                    data = b''.join(unwrap(open(os.path.join(part_dir, f'part.{n}'), 'rb').read(), block)
                                    for n in obj['PartNums'])
                else:
                    blob = next(iter(inline.values())) if inline else b''
                    data = unwrap(blob, block)
                if len(data) != size:
                    raise ValueError(f'kích thước {len(data)} != metadata {size}')
                ctype = (obj.get('MetaUsr') or {}).get('content-type', 'application/octet-stream')
                out = os.path.join(dst, bucket, key)
                os.makedirs(os.path.dirname(out), exist_ok=True)
                open(out, 'wb').write(data)
                open(out + '.content-type', 'w').write(ctype)
                ok += 1
            except Exception as e:  # noqa: BLE001 - báo từng file, không dừng cả lượt
                bad += 1
                print(f'LỖI {bucket}/{key}: {e}')
    print(f'xong: {ok} file OK, {bad} lỗi')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
