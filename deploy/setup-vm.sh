#!/usr/bin/env bash
# Chuẩn bị máy ảo Ubuntu MỚI (chạy 1 lần, trên máy ảo — push-data.sh tự gọi qua SSH):
# cài Docker, tạo thư mục /opt/vsl, thêm 4GB swap (7 JVM + model AI khá tốn RAM).
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo ">> Cài Docker"
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

sudo mkdir -p /opt/vsl/models
sudo chown -R "$USER":"$USER" /opt/vsl

if ! swapon --show | grep -q /swapfile; then
  echo ">> Tạo swap 4GB"
  sudo fallocate -l 4G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo ">> Máy ảo đã sẵn sàng (Docker: $(sudo docker --version))"
