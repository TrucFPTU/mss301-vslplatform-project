"""Kiểm thử đầu-cuối qua API gateway (http://localhost:8080). Chạy: python -X utf8 scripts/e2e_smoke.py
Cần stack đang chạy (docker compose up -d). Tự tạo người dùng / bài viết thử bằng tài khoản admin mặc định (dev)."""
import json
import struct
import time
import urllib.error
import urllib.request
import uuid
import zlib

GW = 'http://localhost:8080'
results = []


def call(method, path, token=None, body=None, files=None, base=GW):
    headers = {}
    data = None
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if files is not None:
        boundary = uuid.uuid4().hex
        parts = []
        for name, (filename, ctype, content) in files.items():
            parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
                          f'Content-Type: {ctype}\r\n\r\n').encode() + content + b'\r\n')
        data = b''.join(parts) + f'--{boundary}--\r\n'.encode()
        headers['Content-Type'] = f'multipart/form-data; boundary={boundary}'
    elif body is not None:
        data = json.dumps(body).encode()
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(base + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw[:1] in (b'{', b'[') else raw)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw


def check(name, cond, detail=''):
    results.append((name, bool(cond), detail))
    print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else f'  -> {detail}'))


def png():
    raw = b'\x00\xff\x00\x00'
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))


def data(resp):
    return resp[1].get('data') if isinstance(resp[1], dict) else None


tag = uuid.uuid4().hex[:6]

# ---------------- Lỗi chuẩn hóa ----------------
s, b = call('GET', '/api/notifications')
check('Không token -> 401 JSON', s == 401 and isinstance(b, dict) and b.get('code') == 'AUTH_1001', (s, b))
s, b = call('GET', '/api/blogs/99999999')
check('Bài không tồn tại -> 404 BLOG_NOT_FOUND', s == 404 and b.get('code') == 'SOCIAL_4001', (s, b))
s, b = call('GET', '/api/blogs/abc')
check('Sai kiểu tham số -> 400 (không còn 500)', s == 400, (s, b))

# ---------------- Đăng nhập + tạo người dùng thử ----------------
s, b = call('POST', '/api/auth/login', body={'username': 'admin', 'password': 'admin123'})
check('Admin đăng nhập', s == 200, (s, b))
admin = data((s, b))['accessToken']
ids = {}
tokens = {}
for u in ['alice', 'bob']:
    s, b = call('POST', '/api/users/admin', admin, {
        'username': f'{u}{tag}', 'email': f'{u}{tag}@test.vn', 'password': 'secret123',
        'fullName': f'{u.title()} Nguyễn', 'role': 'USER'})
    check(f'Admin tạo {u}', s in (200, 201), (s, b))
    ids[u] = data((s, b))['id']
    s, b = call('POST', '/api/auth/login', body={'username': f'{u}{tag}', 'password': 'secret123'})
    tokens[u] = data((s, b))['accessToken']
A, B = tokens['alice'], tokens['bob']

s, b = call('GET', '/api/users/admin', A)
check('USER gọi API admin -> 403 JSON', s == 403 and b.get('code') == 'SYS_0004', (s, b))
s, b = call('GET', f'/api/users/{ids["alice"]}')
check('Hồ sơ công khai không lộ email (khách xem được)', s == 200 and 'email' not in data((s, b)), (s, b))

# ---------------- Avatar ----------------
s, b = call('POST', '/api/users/me/avatar', A, files={'image': ('a.png', 'image/png', png())})
avatar = (data((s, b)) or {}).get('avatarUrl')
check('Upload avatar', s == 200 and avatar, (s, b))
if avatar:
    st, _ = call('GET', '', base=avatar)
    check('Avatar xem được trên MinIO', st == 200, (st, avatar))
s, b = call('POST', '/api/users/me/avatar', A, files={'image': ('x.html', 'text/html', b'<script>')})
check('Upload HTML làm avatar bị từ chối', s == 400, (s, b))

# ---------------- Blog ----------------
title = f'Học bảng chữ cái ký hiệu {tag}'
s, b = call('POST', '/api/blogs', A, {'title': title, 'content': f'Nội dung bài học {tag}', 'status': 'PUBLISHED'})
check('Alice đăng bài (qua kiểm tra trước khi đăng)', s == 201, (s, b))
blog = data((s, b)) or {}
blog_id = blog.get('id')
check('Response có tên tác giả từ identity', (blog.get('author') or {}).get('fullName') == 'Alice Nguyễn', blog.get('author'))
s, b = call('POST', '/api/blogs', B, {'title': title.upper(), 'content': 'khác hẳn', 'status': 'PUBLISHED'})
check('Trùng tiêu đề (khác hoa/thường) -> 409', s == 409 and b.get('code') == 'SOCIAL_4012', (s, b))
s, b = call('POST', f'/api/blogs/{blog_id}/thumbnail', A, files={'image': ('t.png', 'image/png', png())})
check('Upload ảnh bìa', s == 200 and (data((s, b)) or {}).get('thumbnailUrl'), (s, b))

# ---------------- Bình luận / mention / thông báo ----------------
s, b = call('POST', f'/api/blogs/{blog_id}/comments', B, {'content': 'Bài hay quá', 'mentionedUserId': ids['alice']})
comment = data((s, b)) or {}
check('Bob bình luận + nhắc tên Alice', s == 201 and (comment.get('mentionedUser') or {}).get('userId') == ids['alice'], (s, b))
s, b = call('POST', f'/api/comments/{comment.get("id")}/like', A)
check('Like bình luận', s == 200 and data((s, b))['likeCount'] == 1, (s, b))
s, b = call('GET', '/api/notifications', A)
types = [n['type'] for n in (data((s, b)) or {}).get('content', [])]
check('Alice (tác giả bị nhắc tên) nhận đúng 1 thông báo MENTION có tên người gửi',
      types.count('MENTION') == 1 and 'COMMENT' not in types and data((s, b))['content'][0]['actor']['displayName'], types)

# ---------------- Follow / bạn bè / share ----------------
call('POST', f'/api/follows/{ids["alice"]}', B)
s, b = call('POST', f'/api/follows/{ids["bob"]}', A)
check('Theo dõi nhau -> bạn bè', s == 200 and data((s, b))['friend'], (s, b))
s, b = call('POST', '/api/follows/99999999', A)
check('Theo dõi user không tồn tại -> 404', s == 404, (s, b))
s, b = call('POST', f'/api/blogs/{blog_id}/shares', B, {'shareType': 'PROFILE', 'recipientUserId': ids['alice']})
check('Bob chia sẻ lên trang Alice', s == 200 and data((s, b))['blogUrl'].endswith(f'/blogs/{blog_id}'), (s, b))
s, b = call('GET', f'/api/blogs/user/{ids["alice"]}/shared')
check('Trang Alice hiện bài được chia sẻ', s == 200 and data((s, b))['totalElements'] >= 1, (s, b))
s, b = call('GET', f'/api/blogs/search?keyword=Alice')
check('Tìm kiếm trả cả người dùng', s == 200 and any(u['userId'] == ids['alice'] for u in data((s, b))['users']['content']), (s, b))

# ---------------- Tố cáo -> gỡ bài ----------------
s, b = call('POST', f'/api/blogs/{blog_id}/reports', B, {'reason': 'spam'})
check('Bob tố cáo', s == 201, (s, b))
s, b = call('DELETE', f'/api/blogs/{blog_id}', A)
check('Xóa bài đang bị tố cáo -> 409', s == 409 and b.get('code') == 'SOCIAL_4014', (s, b))
s, b = call('GET', '/api/blogs/admin/reports?status=PENDING', admin)
rep = next(r for r in data((s, b))['content'] if r['blogId'] == blog_id)
s, b = call('PUT', f'/api/blogs/admin/reports/{rep["id"]}/resolve', admin, {'reason': 'Quảng cáo trá hình'})
check('Admin xử lý tố cáo -> gỡ bài', s == 200, (s, b))
s, b = call('GET', f'/api/blogs/{blog_id}', A)
check('Tác giả thấy lý do gỡ', s == 200 and data((s, b))['status'] == 'HIDDEN' and data((s, b))['deletionReason'] == 'Quảng cáo trá hình', (s, b))
s, b = call('GET', f'/api/blogs/{blog_id}', B)
check('Người khác không thấy bài đã gỡ', s == 404, (s, b))

# ---------------- Learning ----------------
s, b = call('POST', '/api/categories', admin, {'name': f'Chủ đề {tag}', 'description': 'test'})
check('Admin tạo danh mục', s in (200, 201), (s, b))
cat_id = data((s, b))['id']
s, b = call('POST', '/api/categories', admin, {'name': f'  CHỦ ĐỀ {tag} ', 'description': 'test'})
check('Tên danh mục trùng (hoa/thường) -> 409', s == 409, (s, b))
s, b = call('POST', '/api/vocabularies', admin, {'categoryId': cat_id, 'word': f'xin chào {tag}'})
check('Admin tạo từ vựng', s in (200, 201), (s, b))
vocab_id = data((s, b))['id']
s, b = call('POST', '/api/vocabularies', admin, {'categoryId': cat_id, 'word': f'XIN CHAO {tag}'})
check('Từ trùng (bỏ dấu) -> 409', s == 409, (s, b))
s, b = call('GET', f'/api/vocabularies/exists?word=Xin%20Chao%20{tag}')
check('Kiểm tra từ đã có (bỏ dấu)', s == 200 and data((s, b))['exists'], (s, b))
s, b = call('GET', f'/api/vocabularies/ai-synonyms?word=chao')
check('ai-synonyms cần đăng nhập', s == 401, (s, b))
s, b = call('POST', f'/api/vocabularies/{vocab_id}/image', admin, files={'image': ('v.png', 'image/png', png())})
check('Upload ảnh từ vựng', s == 200 and data((s, b))['imageUrl'], (s, b))
s, b = call('PUT', f'/api/vocabularies/{vocab_id}', admin, {'categoryId': cat_id, 'word': f'xin chào {tag}', 'description': 'sửa'})
check('Sửa từ vựng không làm mất ảnh', s == 200 and data((s, b))['imageUrl'], (s, b))
s, b = call('POST', f'/api/vocabulary-suggestions', A, {'categoryId': cat_id, 'word': f'Xin Chào {tag}'})
check('Đề xuất từ đã có -> 409', s == 409, (s, b))

# ---------------- Thống kê tổng hợp (gọi chéo service) ----------------
s, b = call('GET', '/api/public/stats')
check('Public stats (learning gọi identity)', s == 200 and data((s, b))['totalUsers'] is not None, (s, b))
call('POST', '/api/visits/track', None)
time.sleep(1)
s, b = call('GET', '/api/visits/admin/summary', admin)
d = data((s, b)) or {}
check('Dashboard admin (gọi identity + social)', s == 200 and d.get('totalStudents') is not None and d.get('totalBlogs') is not None and d.get('totalVisits', 0) >= 1, (s, b))

# ---------------- Xóa tài khoản ----------------
s, b = call('DELETE', '/api/users/me', B, {'password': 'sai'})
check('Xóa tài khoản sai mật khẩu -> từ chối', s == 401, (s, b))
s, b = call('DELETE', '/api/users/me', B, {'password': 'secret123'})
check('Bob xóa tài khoản', s == 200, (s, b))
s, b = call('POST', '/api/auth/login', body={'username': f'bob{tag}', 'password': 'secret123'})
check('Tài khoản đã xóa không đăng nhập được', s == 401, (s, b))
s, b = call('GET', f'/api/users/{ids["bob"]}')
check('Tên hiển thị thành "Người dùng đã xóa"', data((s, b))['fullName'] == 'Người dùng đã xóa', (s, b))

passed = sum(1 for _, ok, _ in results if ok)
print(f'\n==> {passed}/{len(results)} PASS')
