# API 테스트 결과 기록

> API 명세(URL 매핑)별 테스트 결과를 기록하는 문서입니다.
> 자동화 테스트는 `src/test/java/com/doumi/donation/api/` 의 컨트롤러별 테스트와 1:1 대응합니다.

## 테스트 정보

| 항목 | 값 |
|---|---|
| 테스터 | JUnit 자동화 (Gradle `:test`) |
| 테스트 일시 | 2026-06-12 |
| 커밋 | `052e007` (북마크/알림 + 단체 승인/사용 보고 도메인 반영) |
| 테스트 방식 | 자동화(JUnit) ☑ / 수동(서버 실행 후 실제 호출) ☐ |

## 표기 규칙

- **결과**: ✅ 통과 / ❌ 실패 / ⏭️ 미실행
- 수동 테스트라면 **실제 응답** 칸에 받은 HTTP 상태코드를 적습니다.
- 실패 시 **비고**에 원인·재현 방법을 적습니다.

---

## 1. 회원 — `MemberControllerTest` (5/5 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/members` | 201 회원가입 | 201 | ✅ | |
| GET | `/api/members/{email}` | 200 내 정보 조회 (본인만) | 200 | ✅ | |
| GET | `/api/members/{email}/donations` | 200 내 기부 내역 (본인만) | 200 | ✅ | |
| PATCH | `/api/members/{email}` | 200 내 정보 수정 (본인만) | 200 | ✅ | |
| DELETE | `/api/members/{email}` | 200 회원 탈퇴 (본인만) | 200 | ✅ | |

## 2. 인증 — `AuthControllerTest` (1/1 통과, 로그인은 수동 대상)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/v1/auth/login` | 200 로그인 + 토큰 발급 | | ⏭️ | 필터 처리라 JUnit 미커버 → 서버 띄우고 수동 테스트 |
| POST | `/api/v1/auth/refresh` | 200 토큰 재발급(RTR) | 200 | ✅ | |

## 3. 캠페인 — `CampaignsControllerTest` (6/6 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/campaigns` | 200 목록 조회(필터 가능) | 200 | ✅ | category/region/status/keyword |
| GET | `/api/campaigns/{id}` | 200 상세 조회 | 200 | ✅ | 없으면 404 |
| POST | `/api/campaigns` | 201 등록 (단체/관리자) | 201 | ✅ | 토큰 memberId가 ownerId로 기록되는 것까지 검증 |
| PATCH | `/api/campaigns/{id}` | 200 수정 (단체/관리자) | 200 | ✅ | |
| DELETE | `/api/campaigns/{id}` | 200 삭제 (관리자) | 200 | ✅ | |
| GET | `/api/campaigns/{id}/donations` | 200 캠페인 기부 내역 | 200 | ✅ | |

## 4. 게시판 — `BoardControllerTest` (6/6 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/board` | 200 목록 조회(페이징) | 200 | ✅ | page/size/category |
| GET | `/api/board/{id}` | 200 상세 조회 | 200 | ✅ | 없으면 404, 조회수 증가 |
| POST | `/api/board` | 201 게시글 등록 (로그인) | 201 | ✅ | |
| PUT | `/api/board/{id}` | 200 수정 (작성자) | 200 | ✅ | 타인이면 403 |
| DELETE | `/api/board/{id}` | 200 삭제 (작성자) | 200 | ✅ | 타인이면 403 |
| POST | `/api/board/{id}/like` | 200 좋아요 토글 (로그인) | 200 | ✅ | 비로그인 401 |

## 5. 댓글 — `CommentControllerTest` (4/4 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/board/{boardId}/comments` | 200 댓글 목록 | 200 | ✅ | 게시글 없으면 404 |
| POST | `/api/board/{boardId}/comments` | 201 댓글 등록 (로그인) | 201 | ✅ | |
| PUT | `/api/board/comments/{commentId}` | 200 수정 (작성자) | 200 | ✅ | 타인이면 403 |
| DELETE | `/api/board/comments/{commentId}` | 200 삭제 (작성자) | 200 | ✅ | 타인이면 403 |

## 6. 포인트 — `PointControllerTest` (3/3 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/points/charge` | 200 포인트 충전 (로그인) | 200 | ✅ | amount ≥ 1 |
| POST | `/api/points/use` | 200 포인트로 기부 (로그인) | 200 | ✅ | 잔액 부족 시 400 |
| GET | `/api/points/history` | 200 충전/사용 이력 (로그인) | 200 | ✅ | |

## 7. 결제 — `PaymentControllerTest` (1/1 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/payments/confirm` | 200 토스 승인 + 충전 (로그인) | 200 | ✅ | 토스 API는 mock — 실결제는 수동 테스트 필요 |

## 8. 랭킹 — `RankingControllerTest` (1/1 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/rankings?yearMonth=` | 200 월별 기부 랭킹 | 200 | ✅ | limit 기본 10 |

## 9. 통계 — `StatsControllerTest` (1/1 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/stats` | 200 전체 통계 | 200 | ✅ | |

## 10. 챗봇 — `ChatbotControllerTest` (2/2 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/chatbot/chat` | 200 대화 + 캠페인 추천 | 200 | ✅ | LLM/Redis는 mock — 실연동은 수동 테스트 필요 |
| POST | `/api/admin/chatbot/reindex` | 200 벡터 DB 재색인 (관리자) | 200 | ✅ | |

## 11. 북마크(찜) — `BookmarkControllerTest` (3/3 통과) 🆕

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/bookmarks` | 200 내가 찜한 캠페인 목록 (로그인) | 200 | ✅ | |
| POST | `/api/bookmarks/{campaignId}` | 201 캠페인 찜 (로그인) | 201 | ✅ | |
| DELETE | `/api/bookmarks/{campaignId}` | 200 찜 해제 (로그인) | 200 | ✅ | |

## 12. 알림 — `NotificationControllerTest` (3/3 통과) 🆕

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/notifications` | 200 내 알림 목록 (로그인) | 200 | ✅ | |
| PATCH | `/api/notifications/{id}/read` | 200 알림 읽음 처리 (로그인) | 200 | ✅ | |
| PATCH | `/api/notifications/read-all` | 200 모든 알림 읽음 처리 (로그인) | 200 | ✅ | |

## 13. 기부금 사용 보고 — `ReportControllerTest` (3/3 통과) 🆕

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/campaigns/{campaignId}/report` | 200 사용 보고 조회 (공개) | 200 | ✅ | 없으면 404 |
| POST | `/api/campaigns/{campaignId}/report` | 201 사용 보고 작성 (소유 단체/관리자) | 201 | ✅ | 지출 항목 1개 이상 필수 |
| POST | `/api/campaigns/{campaignId}/report` | 400 검증 실패(내용/지출 누락) | 400 | ✅ | |

## 14. 단체 승인 관리 — `OrganizationAdminControllerTest` (3/3 통과) 🆕

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| GET | `/api/admin/organizations` | 200 단체 목록 (관리자, status 필터) | 200 | ✅ | PENDING/APPROVED/REJECTED |
| PATCH | `/api/admin/organizations/{memberId}/approve` | 200 단체 승인 (관리자) | 200 | ✅ | |
| PATCH | `/api/admin/organizations/{memberId}/reject` | 200 단체 거절 (관리자, 사유 선택) | 200 | ✅ | |

## 15. 관리자 배치 — `AdminBatchControllerTest` (1/1 통과)

| Method | URL | 기대 | 실제 응답 | 결과 | 비고 |
|--------|-----|------|----------|------|------|
| POST | `/api/admin/public-data` | 202 공공데이터 배치 시작 (관리자) | 202 | ✅ | 중복 실행 시 409 |

---

## 결과 요약

| 구분 | 전체 | ✅ 통과 | ❌ 실패 | ⏭️ 미실행 |
|---|---|---|---|---|
| API 엔드포인트 | 44 | 43 | 0 | 1 (로그인 — 수동 필요) |

### 함께 실행된 서비스 단위 테스트 (팀 작성)

| 테스트 | 결과 |
|---|---|
| `CampaignsServiceImpTest` (벡터 동기화 로직, 5개) | ✅ 전부 통과 |
| `ChatbotServiceImpTest` (환각 방지 로직, 4개) | ✅ 전부 통과 |
| `CampaignIndexServiceImpTest` (재색인 배치, 4개) | ✅ 전부 통과 |
| `JWTUtilTest` (토큰 생성/위변조 거부, 3개) | ✅ 전부 통과 |
| `DonationApplicationTests.contextLoads` | ❌ 실패 — **Redis(6379) 미기동 환경 문제**, 코드 문제 아님. Redis 켜고 실행하면 통과 |

## 발견된 이슈

| 번호 | 관련 API | 내용 | 조치 |
|---|---|---|---|
| 1 | (전체) | `contextLoads`가 Redis 미기동 시 실패 → CI/로컬에서 전체 테스트 돌리려면 Redis 필요 | Redis 기동 후 재실행, 또는 테스트 프로파일 분리 검토 |

---

## 남은 수동 테스트 (서버 + DB/Redis 실행 후)

자동화 테스트는 컨트롤러 계층(URL 매핑·검증·상태코드)만 검증하므로, 아래는 실제 환경에서 별도 확인이 필요합니다.

- [ ] `POST /api/v1/auth/login` — 실제 가입 계정으로 로그인, 토큰 발급 확인
- [ ] `POST /api/payments/confirm` — 토스 결제위젯에서 발급된 실제 paymentKey로 승인
- [ ] `POST /api/chatbot/chat` — Redis + LLM 연동 상태에서 실제 추천 응답 확인
- [ ] 북마크/알림 — DB 연동 상태에서 찜/알림 흐름 확인 (배치 스케줄러 포함)

## 자동화 테스트 실행 방법

```bash
# backend 폴더에서 API 테스트만 (DB/Redis 불필요)
./gradlew test --tests "com.doumi.donation.api.*"

# 특정 컨트롤러만
./gradlew test --tests "com.doumi.donation.api.BookmarkControllerTest"

# 전체 테스트 (contextLoads 포함 — Redis 필요)
./gradlew test
```

- 결과 리포트(HTML): `build/reports/tests/test/index.html`
- API 테스트는 서비스 계층 mock이므로 **DB/Redis 없이 실행 가능**
