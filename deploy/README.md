# Deploy lên Azure (1 máy ảo + Docker Compose + GitHub Actions)

```
Trình duyệt ──https──► Caddy (80/443, tự xin chứng chỉ)
                         ├─ /files/*  → MinIO (ảnh/video)
                         └─ còn lại   → FE nginx ─ /api → api-gateway → các service (qua Eureka)
```

- **CI** (`.github/workflows/ci.yml`): mỗi lần push/PR → `mvn verify` (build + unit test).
- **CD** (`.github/workflows/deploy.yml`): push lên `main` (hoặc bấm tay *Run workflow*) → build 7 image service +
  image FE (lấy từ repo `NTBinh2005/mss301-vclplatform-fe`) → đẩy lên GHCR → SSH vào máy ảo, `docker compose pull && up -d`
  → chờ `https://DOMAIN/api/categories` trả 200.
- Chỉ mở cổng 22/80/443. Postgres, MinIO console, Eureka, Swagger **không** mở ra ngoài — cần xem thì SSH vào
  máy ảo rồi dùng `docker exec` / `docker logs` (vd. `docker exec -it vsl-postgres psql -U vsl vsl_learning`).
- HTTPS là bắt buộc: trình duyệt chỉ cho mở camera (Phòng tập) trên trang https.

## Làm 1 lần

### 1. Tạo máy ảo (Azure Portal → Virtual machines → Create)
- Image **Ubuntu Server 24.04 LTS**, size **B2ms** (2 vCPU, 8GB RAM) — 4GB không đủ cho 7 JVM + model AI.
  Gợi ý vùng: Southeast Asia.
- Authentication: **SSH public key** → tải file `.pem` về (đây là `VM_SSH_KEY`). Username mặc định `azureuser`.
- Inbound ports: **SSH (22), HTTP (80), HTTPS (443)**.
- Disk: Standard SSD 30GB là đủ.
- Tạo xong: VM → Overview → **DNS name → Configuration** → đặt nhãn, vd. `vsl-demo`
  ⇒ tên miền `vsl-demo.southeastasia.cloudapp.azure.com`. (Có domain riêng thì trỏ bản ghi A về IP máy ảo.)
- 💸 Không dùng thì **Stop (deallocate)** máy ảo để không bị trừ tiền CPU/RAM (chỉ còn tiền ổ đĩa).
  Bật lại IP công khai có thể đổi, nhưng tên DNS `cloudapp.azure.com` vẫn giữ.

### 2. Điền `deploy/.env`
Chép `deploy/.env.example` → `deploy/.env` (đã gitignore), sửa `DOMAIN` cho đúng tên miền ở bước 1.
Mật khẩu DB/MinIO/JWT tạo mới bằng `openssl rand` (không dùng giá trị dev `vsl`/`minioadmin`).

### 3. Đẩy cấu hình, model AI và dữ liệu lên máy ảo (Git Bash, từ thư mục gốc repo)
```bash
chmod 600 ~/Downloads/vsl-vm_key.pem
eval "$(ssh-agent -s)" && ssh-add ~/Downloads/vsl-vm_key.pem
bash deploy/push-data.sh azureuser@<IP-may-ao> --with-data
```
- Script cài Docker + swap 4GB, chép `.env`, compose, Caddyfile, model `models/mvitv2_small.onnx` (~135MB).
- `--with-data`: lấy DB + ảnh/video đang có ở Docker local, đổi link `http://localhost:9000/...` →
  `https://DOMAIN/files/...`, rồi **ghi đè** dữ liệu trên máy ảo. Bỏ `--with-data` nếu chỉ cập nhật `.env`/model.
- Đổi `.env` sau này: sửa `deploy/.env` → chạy lại script (không `--with-data`) → chạy workflow Deploy.

### 4. Secrets cho GitHub Actions
Repo backend → Settings → Secrets and variables → Actions → *New repository secret*:

| Secret | Giá trị |
|---|---|
| `VM_HOST` | IP hoặc tên miền máy ảo |
| `VM_USER` | `azureuser` |
| `VM_SSH_KEY` | toàn bộ nội dung file `.pem` |

Rồi Actions → **Deploy** → *Run workflow* (chọn nhánh FE, mặc định `main`).
Image được đẩy lên `ghcr.io/trucfptu/vsl-*`; máy ảo đăng nhập GHCR bằng token tạm của chính lần chạy đó.

## Hằng ngày
- Merge vào `main` → tự deploy. Chỉ FE đổi → bấm tay *Run workflow*.
- Xem log: `ssh azureuser@IP "cd /opt/vsl && docker compose -f docker-compose.prod.yml logs -f --tail 100 learning-service"`.
- Quay lại bản cũ: đặt `IMAGE_TAG=<7 ký tự sha>` trong `/opt/vsl/.env` → `docker compose ... up -d`.

## Lưu ý
- **OTP đăng ký (Resend)**: chưa xác minh domain thì chỉ gửi được tới email chủ tài khoản Resend ⇒ người khác không
  đăng ký được. Xác minh domain trên resend.com, đặt `RESEND_FROM_EMAIL=SignMentor <no-reply@domain>`.
  (Tên `cloudapp.azure.com` không xác minh được — cần domain riêng.)
- Dữ liệu nằm trong volume Docker trên máy ảo (`pgdata`, `miniodata`) — xóa máy ảo là mất. Sao lưu:
  `docker exec vsl-postgres pg_dump -U vsl vsl_learning > backup.sql`.
