#!/usr/bin/env bash
# ============================================================
#  도우미(donation) 배포 스크립트  (최종본)
#  그동안 겪은 이슈를 모두 반영:
#   - 프론트 npm install (package-lock 부재)        → Dockerfile 반영됨
#   - MySQL 대소문자 구분 (lower-case-table-names)  → docker-compose 반영됨
#   - t3.small 메모리 부족 (JVM/MySQL 메모리 제한)   → docker-compose 반영됨
#   - 한글 인코딩 깨짐 (utf8mb4 import)             → 이 스크립트가 처리
#   - 스키마 import 누락                            → 자동 확인 후 import
#
#  사용법:
#    bash deploy.sh           # 배포/업데이트 (DB 데이터 유지)
#    bash deploy.sh --reset   # DB까지 싹 지우고 처음부터 (데이터 삭제!)
#
#  사전 준비(최초 1회):
#    1) Docker 설치 (server-setup.sh 참고)
#    2) .env 작성:  cp .env.example .env  &&  nano .env
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

# docker가 sudo 없이 되면 그대로, 안 되면 sudo 사용
DC="docker compose"
docker info >/dev/null 2>&1 || DC="sudo docker compose"

# .env 확인
if [ ! -f .env ]; then
  echo "❌ .env 파일이 없습니다. 먼저 만드세요:"
  echo "   cp .env.example .env && nano .env"
  exit 1
fi

# --reset : DB 볼륨까지 삭제하고 처음부터
if [ "${1:-}" = "--reset" ]; then
  echo "⚠  --reset: 모든 볼륨(DB 포함)을 삭제합니다."
  $DC down -v
fi

echo "▶ 1/4  컨테이너 빌드 & 기동 (첫 빌드는 몇 분 걸립니다)"
$DC up -d --build

echo "▶ 2/4  MySQL 준비 대기"
until echo "SELECT 1;" | $DC exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' >/dev/null 2>&1; do
  printf '.'; sleep 3
done
echo " ✓ MySQL 준비됨"

echo "▶ 3/4  DB 스키마 확인 / 초기화"
HAS=$(echo "SHOW TABLES IN donation LIKE 'member';" \
      | $DC exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N' 2>/dev/null || true)
if [ -z "$HAS" ]; then
  echo "   → member 테이블이 없어 스키마를 import 합니다 (utf8mb4)"
  $DC exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' \
      < backend/sql/donation.sql
  $DC restart backend
  echo "   ✓ import 완료 + 백엔드 재시작"
else
  echo "   → 이미 초기화돼 있어 건너뜁니다 (기존 데이터 보존)"
fi

echo "▶ 4/4  헬스 체크 (백엔드 부팅 대기)"
sleep 15
for i in 1 2 3; do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/campaigns || echo "000")
  echo "   api=$code"
  sleep 2
done

echo ""
echo "✅ 배포 완료!  브라우저에서  http://<서버_퍼블릭_IP>  접속"
echo "   (api=200 이 나오면 정상입니다)"
