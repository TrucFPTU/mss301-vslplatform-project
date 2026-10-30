#!/usr/bin/env bash
# Chạy trên MÁY DEV (Git Bash), từ thư mục gốc repo:
#   bash deploy/push-data.sh azureuser@<ip-may-ao>               # cài máy ảo + chép .env và model AI
#   bash deploy/push-data.sh azureuser@<ip-may-ao> --with-data   # ... + chép DB và ảnh/video từ Docker local
#
# --with-data GHI ĐÈ toàn bộ DB/file trên máy ảo bằng dữ liệu local (dùng cho lần deploy đầu).
# Link ảnh/video trong DB (http://localhost:9000/...) được đổi sang https://DOMAIN/files/...
set -euo pipefail

VM="${1:?Cách dùng: bash deploy/push-data.sh user@host [--with-data]}"
WITH_DATA="${2:-}"
REMOTE=/opt/vsl
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_PROD="$ROOT/deploy/.env"
[ -f "$ENV_PROD" ] || { echo "Thiếu deploy/.env (chép từ deploy/.env.example)"; exit 1; }
envget() { grep "^$1=" "$2" | head -1 | cut -d= -f2- | tr -d '\r'; }
DOMAIN=$(envget DOMAIN "$ENV_PROD")

echo ">> [1] Chuẩn bị máy ảo"
ssh "$VM" 'bash -s' < "$ROOT/deploy/setup-vm.sh"

echo ">> [2] Chép cấu hình + model AI + image MinIO"
# MinIO không còn image tải được từ Docker Hub -> chép bản máy dev đang có (ghim trong docker-compose.prod.yml).
MINIO_IMAGE=$(grep -m1 -o 'minio/minio:RELEASE[^[:space:]]*' "$ROOT/deploy/docker-compose.prod.yml")
if ! ssh "$VM" "sudo docker image inspect $MINIO_IMAGE >/dev/null 2>&1"; then
  docker image inspect "$MINIO_IMAGE" >/dev/null 2>&1 || docker tag minio/minio:latest "$MINIO_IMAGE"
  docker save "$MINIO_IMAGE" | gzip -1 | ssh "$VM" "gunzip | sudo docker load"
fi
# Bỏ \r (file sửa trên Windows / openssl của Git Bash): lọt \r vào JWT_SECRET là service không khởi động được.
tr -d '\r' < "$ENV_PROD" | ssh "$VM" "cat > $REMOTE/.env && chmod 600 $REMOTE/.env"
scp "$ROOT/deploy/docker-compose.prod.yml" "$ROOT/deploy/Caddyfile" "$ROOT/scripts/init-multiple-databases.sql" "$VM:$REMOTE/"
scp "$ROOT/models/mvitv2_small.onnx" "$VM:$REMOTE/models/"

[ "$WITH_DATA" = "--with-data" ] || { echo ">> Xong (không chép dữ liệu)."; exit 0; }

# ---------- Xuất dữ liệu local ----------
LOCAL_DB_USER=$(envget DB_USERNAME "$ROOT/.env"); LOCAL_DB_USER=${LOCAL_DB_USER:-vsl}
LOCAL_MINIO_USER=$(envget MINIO_USER "$ROOT/.env"); LOCAL_MINIO_USER=${LOCAL_MINIO_USER:-minioadmin}
LOCAL_MINIO_PASS=$(envget MINIO_PASSWORD "$ROOT/.env"); LOCAL_MINIO_PASS=${LOCAL_MINIO_PASS:-minioadmin}
OLD_MEDIA=$(envget MINIO_PUBLIC_ENDPOINT "$ROOT/.env"); OLD_MEDIA=${OLD_MEDIA:-http://localhost:9000}
TMP="$ROOT/deploy/.export"
rm -rf "$TMP" && mkdir -p "$TMP"

echo ">> [3] Xuất DB + file từ Docker local"
(cd "$ROOT" && docker compose up -d postgres minio)
until docker exec vsl-postgres pg_isready -U "$LOCAL_DB_USER" >/dev/null 2>&1; do sleep 2; done
for db in vsl_identity vsl_learning vsl_social; do
  docker exec vsl-postgres pg_dump -U "$LOCAL_DB_USER" --no-owner --no-acl "$db" \
    | sed "s#${OLD_MEDIA%/}/#https://$DOMAIN/files/#g" > "$TMP/$db.sql"
done
docker exec vsl-minio sh -c "rm -rf /tmp/export && mkdir -p /tmp/export && mc alias set l http://localhost:9000 '$LOCAL_MINIO_USER' '$LOCAL_MINIO_PASS' >/dev/null   && mc mirror --quiet l /tmp/export >/dev/null"
# Git Bash trên Windows: docker cần đường dẫn kiểu Windows và không được tự đổi "/tmp/..." trong container.
HOST_TMP=$(cygpath -w "$TMP" 2>/dev/null || echo "$TMP")
MSYS_NO_PATHCONV=1 docker cp vsl-minio:/tmp/export "$HOST_TMP/minio"
MSYS_NO_PATHCONV=1 docker exec vsl-minio rm -rf /tmp/export
du -sh "$TMP"/*

echo ">> [4] Chép lên máy ảo + nạp"
ssh "$VM" "mkdir -p $REMOTE/import"
scp -r "$TMP"/* "$VM:$REMOTE/import/"
ssh "$VM" 'bash -s' <<EOF
set -euo pipefail
cd $REMOTE
DC="sudo docker compose -f docker-compose.prod.yml --env-file .env"
# Dừng service nghiệp vụ (nếu đang chạy) để không ghi DB trong lúc nạp.
\$DC stop identity-service learning-service social-service 2>/dev/null || true
\$DC up -d postgres minio
until sudo docker exec vsl-postgres pg_isready -U "\$(grep '^DB_USERNAME=' .env | cut -d= -f2)" >/dev/null 2>&1; do sleep 2; done
U=\$(grep '^DB_USERNAME=' .env | cut -d= -f2)
for db in vsl_identity vsl_learning vsl_social; do
  sudo docker exec vsl-postgres psql -U "\$U" -d postgres -qc "DROP DATABASE IF EXISTS \$db WITH (FORCE);" -c "CREATE DATABASE \$db;"
  sudo docker exec -i vsl-postgres psql -U "\$U" -d "\$db" -q -v ON_ERROR_STOP=1 < import/\$db.sql >/dev/null
done
MU=\$(grep '^MINIO_USER=' .env | cut -d= -f2); MP=\$(grep '^MINIO_PASSWORD=' .env | cut -d= -f2)
sudo docker exec vsl-minio rm -rf /tmp/import
sudo docker cp import/minio vsl-minio:/tmp/import
sudo docker exec vsl-minio sh -c "mc alias set r http://localhost:9000 '\$MU' '\$MP' >/dev/null   && mc mirror --quiet --overwrite /tmp/import r >/dev/null && rm -rf /tmp/import"
rm -rf import
# Khởi động lại toàn bộ (nếu image đã có trên máy ảo). Service tự đặt quyền đọc công khai cho bucket khi chạy.
\$DC up -d 2>/dev/null || echo "Chưa có image — chạy workflow Deploy trên GitHub để kéo image."
EOF
rm -rf "$TMP"
echo ">> Xong. Mở https://$DOMAIN"
