#!/usr/bin/env bash
# ============================================================
#  새 Ubuntu 서버(EC2) 최초 1회 세팅
#   - swap 2GB (메모리 보강)
#   - Docker + Docker Compose v2 + git 설치
#   - 현재 사용자를 docker 그룹에 추가
#
#  사용법:  bash server-setup.sh
#  실행 후: 터미널 재접속(로그아웃→재접속) → deploy.sh 실행
# ============================================================
set -e

echo "▶ 패키지 목록 갱신"
sudo apt update

echo "▶ swap 2GB 설정 (없을 때만)"
if ! sudo swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  echo "  ✓ swap 추가됨"
else
  echo "  → swap 이미 있음, 건너뜀"
fi

echo "▶ Docker / Compose / git 설치"
# Ubuntu 24.04 기준: docker-compose-plugin 이 아니라 docker-compose-v2 패키지명 사용
sudo apt install -y docker.io docker-compose-v2 git

echo "▶ docker 그룹에 사용자 추가 (sudo 없이 docker 쓰기)"
sudo usermod -aG docker "$USER"

echo ""
echo "✅ 서버 세팅 완료!"
echo "   ⚠ 터미널을 완전히 닫고 재접속한 뒤 'docker --version' 확인 → deploy.sh 실행"
