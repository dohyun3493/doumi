# 📋 도우미 API 명세서

**Base URL:** `http://localhost:8080`
**공통 prefix:** 모든 API는 `/api` 로 시작

> 마지막 갱신 기준: 전체 경로 `/api` 통일 + JWT 인증 + ADMIN 역할 분리

---

## 🔐 인증 `/api/v1/auth`

| 메서드 | 경로 | 설명 | 요청 | 응답 |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | 로그인 (JWT 발급) | `{email, password}` | `{accessToken, refreshToken}` |
| `POST` | `/api/v1/auth/refresh` | 토큰 재발급 | Header: `Refresh-Token` | `{accessToken, refreshToken}` |
| `POST` | `/api/v1/auth/kakao` | 카카오 로그인 | `{code, redirectUri}` | `{accessToken, refreshToken}` |
| `POST` | `/api/v1/auth/email/send-code` | 회원가입 이메일 인증코드 발송 (미가입 이메일만) | `{email}` | `{message}` |
| `POST` | `/api/v1/auth/email/verify-code` | 인증코드 검증 → 가입 가능 표시 | `{email, code}` | `{message}` |
| `POST` | `/api/v1/auth/password/send-code` | 비밀번호 재설정 코드 발송 (가입 이메일만) | `{email}` | `{message}` |
| `POST` | `/api/v1/auth/password/reset` | 코드 검증 후 비밀번호 변경 | `{email, code, newPassword}` | `{message}` |

> - 로그인은 컨트롤러가 아닌 `JWTAuthenticationFilter`가 처리 (`setFilterProcessesUrl`)
> - 이후 요청은 헤더에 `Authorization: Bearer {accessToken}` 첨부
> - Access Token에 `memberId`, `email`, `role` claim 포함
> - **이메일 인증**: 인증코드는 Redis에 TTL로 저장(가입 5분 / 재설정 30분), 발송은 Gmail SMTP. 재발송은 60초 쿨다운.
> - **회원가입 전제조건**: `email/verify-code`로 인증을 마친 이메일만 `POST /api/members` 가입 가능. 카카오(`@kakao.local`) 계정은 비밀번호 재설정 대상에서 제외.

---

## 👤 회원 `/api/members`

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `POST` | `/api/members` | 회원가입 | `{email, password, name, memberType}` | 🟢 공개 |
| `GET` | `/api/members/{email}` | 회원 정보 조회 | - | 🔒 인증 |
| `PATCH` | `/api/members/{email}` | 회원 정보 수정 | `{email?, password?, name?}` | 🔒 인증 |
| `DELETE` | `/api/members/{email}` | 회원 탈퇴 | - | 🔒 인증 |
| `GET` | `/api/members/{email}/donations` | 회원별 기부 내역 | - | 🔒 인증 |

- `memberType`: `INDIVIDUAL` | `ORGANIZATION` (가입 시 ADMIN 불가)
- 회원 조회 응답: `{memberId, email, name, memberType, pointBalance}`
- 기부 내역 응답: `[{donationId, campaignTitle, campaignId, amount, donatedAt}]`

---

## 💎 포인트 `/api/points`

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `POST` | `/api/points/charge` | 포인트 충전 | `{email, amount}` | 🔒 인증 |
| `POST` | `/api/points/use` | 포인트로 기부 | `{email, campaignId, amount}` | 🔒 인증 |
| `GET` | `/api/points/history/{email}` | 충전/사용 내역 | - | 🔒 인증 |

- `type`: `CHARGE`(충전) | `USE`(사용)
- 내역 응답: `[{historyId, memberId, type, amount, createdAt}]`

---

## 💳 결제 `/api/payments`

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `POST` | `/api/payments/confirm` | 토스 결제 승인 + 포인트 충전 | `{paymentKey, orderId, amount, email}` | 🔒 인증 |

> 토스페이먼츠 결제위젯 → successUrl 리다이렉트 후 호출. 시크릿 키로 토스 승인 API 검증 후 충전.

---

## 📢 캠페인 `/api/campaigns`

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `GET` | `/api/campaigns` | 목록 조회 (필터/검색) | Query: `category?, region?, status?, keyword?` | 🟢 공개 |
| `GET` | `/api/campaigns/{id}` | 상세 조회 | - | 🟢 공개 |
| `GET` | `/api/campaigns/{id}/donations` | 캠페인 기부자 목록 | - | 🟢 공개 |
| `POST` | `/api/campaigns` | 캠페인 등록 | `Campaign` | 🔒 인증 |
| `PATCH` | `/api/campaigns/{campaignId}` | 수정 (부분) | `Campaign` | 🔒 인증 |
| `DELETE` | `/api/campaigns/{campaignId}` | 삭제 | - | 🔒 인증 |

- `maskedName`: 기부자 이름 마스킹 (예: `김*수`)
- `status`: `모집중` | `모집예정` | `모집완료` | `사용완료` 등
- 기부자 응답: `[{donationId, maskedName, amount, donatedAt}]`

---

## 📝 게시판 `/api/board`

### 게시글 (BoardController)

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `GET` | `/api/board` | 목록 (페이징) | Query: `category?, page=1, size=10` | 🟢 공개 |
| `GET` | `/api/board/{id}` | 상세 조회 | - | 🟢 공개 |
| `POST` | `/api/board` | 게시글 등록 | `{title, content, category}` | 🔒 인증 |
| `PUT` | `/api/board/{id}` | 게시글 수정 | `{title, content}` | 🔒 인증 |
| `DELETE` | `/api/board/{id}` | 게시글 삭제 | - | 🔒 인증 |

### 댓글 (CommentController, 동일 `/api/board` prefix)

| 메서드 | 경로 | 설명 | 요청 | 인증 |
|---|---|---|---|---|
| `GET` | `/api/board/{boardId}/comments` | 댓글 목록 | - | 🟢 공개 |
| `POST` | `/api/board/{boardId}/comments` | 댓글 등록 | `{content}` | 🔒 인증 |
| `PUT` | `/api/board/comments/{commentId}` | 댓글 수정 | `{content}` | 🔒 인증 |
| `DELETE` | `/api/board/comments/{commentId}` | 댓글 삭제 | - | 🔒 인증 |

> 작성자는 **JWT 토큰의 memberId**로 식별 (`@AuthenticationPrincipal`). 헤더 위조 불가.
> 목록 응답: `{posts, currentPage, totalPages, totalElements}`

---

## 🏆 랭킹 `/api/rankings`

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| `GET` | `/api/rankings` | 기부 랭킹 조회 (배치 집계) | 🟢 공개 |

---

## 📊 통계 `/api/stats`

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| `GET` | `/api/stats` | 홈/소개 통계 | 🟢 공개 |

- 응답: `{totalDonationAmount, activeCampaigns, totalMembers, monthlyDonors, completedCampaigns}`

---

## ⚙️ 관리자 `/api/admin`

| 메서드 | 경로 | 설명 | 권한 | 응답 |
|---|---|---|---|---|
| `POST` | `/api/admin/public-data` | 공공데이터 배치 수집 시작 | 🔑 `ROLE_ADMIN` | 202 / 409(중복 실행) |

---

## 🔒 인증 정책 (SecurityConfig 기준)

| 구분 | 경로 |
|---|---|
| 🟢 **공개 (permitAll)** | `/api/v1/auth/**`, `/api/public-data/**`, `POST /api/members`(가입), `GET` — `/api/campaigns/**`, `/api/stats/**`, `/api/rankings/**`, `/api/board/**`, `/error` |
| 🔑 **ADMIN 전용** | `/api/admin/**` |
| 🔒 **인증 필요** | 그 외 모든 요청 (포인트/결제/글쓰기/회원정보/캠페인 등록 등) |

### 역할(Role) 체계

| member_type | 권한 |
|---|---|
| `INDIVIDUAL` | 기부, 글쓰기 |
| `ORGANIZATION` | + 캠페인 등록 |
| `ADMIN` | + 공공데이터 배치 등 시스템 운영 (가입 불가, DB 승격) |

---

## 📌 공통 사항

- **인증 헤더**: `Authorization: Bearer {accessToken}`
- **토큰 저장**: localStorage (`accessToken`, `refreshToken`)
- **토큰 만료**: Access Token 30분 / Refresh Token 7일 (RTR 회전 방식)
- **토큰 재발급**: 401 응답 시 프론트가 자동으로 `/api/v1/auth/refresh` 호출 후 재요청
- **에러 응답**: `{ "error": "메시지" }` (JWT 예외 시 401)
- **프론트 prefix**: api.js가 `/api`로 시작하지 않는 요청에 자동으로 `/api` 부착
