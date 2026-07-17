# 배포 가이드 (AWS EC2 + Docker)

EC2(Ubuntu) 한 대에 Docker Compose로 **MySQL + Redis Stack + Spring Boot + Nginx(Vue)** 를 올린다.

## 구성
- `server-setup.sh` — 새 서버 최초 1회 세팅 (swap, Docker 설치)
- `deploy.sh` — 빌드·기동·DB초기화·헬스체크 (반복 실행 가능)
- `docker-compose.yml` — 서비스 정의 (대소문자/메모리 이슈 반영됨)
- `.env.example` — 비밀값 템플릿 (서버에서 `.env`로 복사해 사용)

---

## 최초 배포 (새 서버 기준)

### 1. 서버 접속 후 세팅
```bash
# (코드 클론 전이면 git만 먼저 필요 → server-setup.sh가 설치해줌)
bash server-setup.sh
# → 터미널 완전히 닫고 재접속 (docker 그룹 적용)
```

### 2. 코드 가져오기
GitHub 저장소를 클론한다:
```bash
cd ~
git clone https://github.com/dohyun3493/doumi.git donation
cd donation
```

### 3. 비밀값 작성
```bash
cp .env.example .env
nano .env        # DB_ROOT_PW, LLM_API_KEY 등 실제 값 입력
```

### 4. 배포
```bash
bash deploy.sh
```
→ `api=200` 나오면 완료. 브라우저에서 `http://<서버 퍼블릭 IP>` 접속.

---

## 재배포 / 업데이트
코드 수정 후:
```bash
cd ~/donation
git pull
bash deploy.sh          # DB 데이터 유지하며 재빌드
```

## DB까지 완전 초기화 (데이터 삭제)
```bash
bash deploy.sh --reset
```

---

## 자주 쓰는 명령
```bash
sudo docker compose ps                      # 상태 확인
sudo docker compose logs --tail=50 backend  # 백엔드 로그
sudo docker compose restart backend         # 백엔드만 재시작

# 회원을 ADMIN으로 변경 (예: id=1)
echo "USE donation; UPDATE member SET member_type='ADMIN' WHERE member_id=1;" \
  | sudo docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4'
```

---

## 트러블슈팅 메모 (겪었던 이슈 = 이미 반영됨)
| 증상 | 원인 | 해결(반영 위치) |
|------|------|----------------|
| `npm ci` 실패 | package-lock.json 없음 | `npm install` (frontend/Dockerfile) |
| `Table 'donation.xxx' doesn't exist` | 리눅스 대소문자 구분 (Windows에서 대문자로 생성) | `--lower-case-table-names=1` (docker-compose.yml) → **DB 새로 초기화 필요** |
| 502/500 반복 (백엔드 죽음) | t3.small 메모리 부족(OOM) | JVM `-Xmx512m`, MySQL buffer 축소 (docker-compose.yml) |
| `Invalid default value` / 한글 깨짐 | import 인코딩 | `--default-character-set=utf8mb4` (deploy.sh) |
| 회원가입 500 | 스키마 미초기화/부분 import | `deploy.sh`가 member 테이블 확인 후 자동 import |

> ⚠️ 대소문자 설정은 **MySQL 최초 초기화 시에만** 적용된다. 이미 띄운 적 있으면 `bash deploy.sh --reset` 으로 DB를 새로 만들어야 한다.
