-- =============================================
-- donation 데이터베이스 전체 DDL (초기화 스크립트)
-- 실행하면 기존 DB를 삭제하고 처음부터 다시 생성한다.
-- 과거 1~7차 ALTER 마이그레이션은 모두 각 테이블 정의에 반영(합침)된 상태.
-- =============================================

DROP DATABASE IF EXISTS donation;

CREATE DATABASE IF NOT EXISTS donation
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE donation;

-- 회원 (개인 / 단체 / 관리자)
-- 단체 관련 컬럼(org_*)은 개인 회원이면 NULL.
-- 탈퇴는 행을 지우지 않고 deleted_at만 채우는 soft delete 방식.
CREATE TABLE MEMBER (
    member_id         BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    member_type       ENUM('INDIVIDUAL', 'ORGANIZATION', 'ADMIN') NOT NULL,
    point_balance     BIGINT       NOT NULL DEFAULT 0,
    org_reg_no        VARCHAR(30)  NULL COMMENT '단체 고유번호 (###-##-#####)',
    org_status        ENUM('PENDING', 'APPROVED', 'REJECTED') NULL COMMENT '단체 승인 상태',
    reject_reason     VARCHAR(300) NULL COMMENT '승인 거절 사유',
    profile_image_url VARCHAR(300) NULL COMMENT '프로필 사진 경로 (/uploads/...)',
    deleted_at        DATETIME     NULL COMMENT '탈퇴 일시 (NULL이면 활성 회원)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refresh           VARCHAR(500) NULL,
    PRIMARY KEY (member_id)
);

-- 캠페인 (공공데이터 수집분 + 단체 직접 등록분)
-- owner_id: 직접 등록한 단체 member_id (공공데이터 캠페인은 NULL).
CREATE TABLE CAMPAIGN (
    campaign_id           BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id              BIGINT        NULL         COMMENT '등록 단체 member_id (공공데이터 캠페인은 NULL)',
    api_reg_no            VARCHAR(50)   NULL UNIQUE  COMMENT '모집등록번호 (API 중복방지)',
    org_name              VARCHAR(100)  NULL         COMMENT '단체명 (rcritrNm)',
    title                 VARCHAR(200)  NOT NULL,
    description           TEXT          NULL         COMMENT '모집소개 (rcritSj)',
    purpose               TEXT          NULL         COMMENT '모집목적 (rcritPurps)',
    goal_amount           BIGINT        NOT NULL,
    current_amount        BIGINT        NOT NULL DEFAULT 0,
    region                VARCHAR(100)  NULL,
    category              VARCHAR(100)  NULL,
    start_date            DATE          NOT NULL,
    end_date              DATE          NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT '모집예정' COMMENT '모집중 | 모집예정 | 사용완료 등',
    thumbnail_url         VARCHAR(300)  NULL         COMMENT '대표 이미지 경로 (/uploads/...)',
    homepage_url          VARCHAR(300)  NULL         COMMENT '홈페이지주소 (hmpgAdres)',
    zip_code              VARCHAR(10)   NULL         COMMENT '우편번호 (zip)',
    address               VARCHAR(300)  NULL         COMMENT '주소 (adres)',
    tel_no                VARCHAR(30)   NULL         COMMENT '전화번호 (dmstcTelno)',
    applied_at            DATE          NULL         COMMENT '신청일 (saveDt)',
    delete_requested      BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '삭제 요청 여부',
    delete_request_reason VARCHAR(300)  NULL         COMMENT '삭제 요청 사유',
    PRIMARY KEY (campaign_id),
    CONSTRAINT fk_campaign_owner FOREIGN KEY (owner_id) REFERENCES MEMBER (member_id)
);

-- 기부 내역
CREATE TABLE DONATION (
    donation_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id   BIGINT   NOT NULL,
    campaign_id BIGINT   NOT NULL,
    amount      BIGINT   NOT NULL,
    donated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (donation_id),
    CONSTRAINT fk_donation_member
        FOREIGN KEY (member_id)   REFERENCES MEMBER   (member_id),
    CONSTRAINT fk_donation_campaign
        FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN (campaign_id)
);

-- 포인트 내역 (충전 / 사용 / 환불)
CREATE TABLE POINT_HISTORY (
    history_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    type       ENUM('CHARGE', 'USE', 'REFUND') NOT NULL,
    amount     BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    CONSTRAINT fk_point_member
        FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

-- 결제 원장 (토스 승인 기록. 환불/분쟁 대응의 근거)
CREATE TABLE PAYMENT (
    payment_id  BIGINT       NOT NULL AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    payment_key VARCHAR(200) NOT NULL UNIQUE COMMENT '토스 paymentKey (취소 시 필요)',
    order_id    VARCHAR(100) NOT NULL,
    amount      BIGINT       NOT NULL,
    -- PENDING/CANCELING = 토스 호출 결과를 아직 모르는 상태.
    -- 이 '모름' 상태가 있어야 장애로 중단된 건을 스케줄러가 재조회해 복구할 수 있다.
    status      ENUM('PENDING', 'CHARGED', 'CANCELING', 'CANCELED', 'FAILED')
                             NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 방치된 건을 찾는 기준 (마지막 상태 변경 시각)
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    canceled_at DATETIME     NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES MEMBER (member_id)
);

-- 게시판
CREATE TABLE BOARD (
    board_id    BIGINT       NOT NULL AUTO_INCREMENT,
    campaign_id BIGINT       NULL,
    author_id   BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NULL,
    category    VARCHAR(50)  NOT NULL DEFAULT '일반',
    views       INT          NOT NULL DEFAULT 0,
    likes       INT          NOT NULL DEFAULT 0,
    is_pinned   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (board_id),
    CONSTRAINT fk_board_campaign
        FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN (campaign_id) ON DELETE SET NULL,
    CONSTRAINT fk_board_author
        FOREIGN KEY (author_id)   REFERENCES MEMBER   (member_id) ON DELETE CASCADE
);

-- 게시판 댓글
-- deleted_by: 실제로 지우지 않고 삭제 주체만 기록 → "OO에 의해 삭제된 댓글입니다" 안내.
CREATE TABLE BOARD_COMMENT (
    comment_id  BIGINT       NOT NULL AUTO_INCREMENT,
    board_id    BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL,
    content     TEXT         NOT NULL,
    deleted_by  VARCHAR(20)  NULL COMMENT '삭제 주체: NULL=정상, AUTHOR=작성자, ADMIN=관리자',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_board
        FOREIGN KEY (board_id)  REFERENCES BOARD  (board_id)  ON DELETE CASCADE,
    CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id) REFERENCES MEMBER (member_id) ON DELETE CASCADE
);

-- 게시글 좋아요 매핑
CREATE TABLE BOARD_LIKE (
    like_id    BIGINT   NOT NULL AUTO_INCREMENT,
    board_id   BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (like_id),
    UNIQUE KEY uq_board_member_like (board_id, member_id),
    CONSTRAINT fk_like_board  FOREIGN KEY (board_id)  REFERENCES BOARD  (board_id)  ON DELETE CASCADE,
    CONSTRAINT fk_like_member FOREIGN KEY (member_id) REFERENCES MEMBER (member_id) ON DELETE CASCADE
);

-- 월별 기부 랭킹
CREATE TABLE RANKING (
    ranking_id   BIGINT  NOT NULL AUTO_INCREMENT,
    member_id    BIGINT  NOT NULL,
    `year_month` CHAR(7) NOT NULL COMMENT '예: 2025-05',
    total_amount BIGINT  NOT NULL DEFAULT 0,
    `rank`       INT     NOT NULL,
    PRIMARY KEY (ranking_id),
    UNIQUE KEY uq_ranking_member_month (member_id, `year_month`),
    CONSTRAINT fk_ranking_member
        FOREIGN KEY (member_id) REFERENCES MEMBER (member_id),
    INDEX idx_ranking_month_rank (`year_month`, `rank`),
    INDEX idx_ranking_member (member_id)
);

-- 찜 (관심 캠페인)
CREATE TABLE BOOKMARK (
    bookmark_id BIGINT   NOT NULL AUTO_INCREMENT,
    member_id   BIGINT   NOT NULL,
    campaign_id BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmark_id),
    UNIQUE KEY uq_bookmark (member_id, campaign_id),
    CONSTRAINT fk_bookmark_member   FOREIGN KEY (member_id)   REFERENCES MEMBER   (member_id)   ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_campaign FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN (campaign_id) ON DELETE CASCADE
);

-- 알림 (마감 임박, 목표 달성, 사용보고, 단체승인 등)
CREATE TABLE NOTIFICATION (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT,
    member_id       BIGINT       NOT NULL,
    type            VARCHAR(30)  NOT NULL COMMENT 'DEADLINE | GOAL_ACHIEVED 등',
    content         VARCHAR(300) NOT NULL,
    link_url        VARCHAR(200) NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    INDEX idx_notification_member (member_id, is_read),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES MEMBER (member_id) ON DELETE CASCADE
);

-- 기부금 사용 보고 (캠페인당 1개, 등록 시 캠페인이 '사용완료'로 전환됨)
CREATE TABLE CAMPAIGN_REPORT (
    report_id   BIGINT   NOT NULL AUTO_INCREMENT,
    campaign_id BIGINT   NOT NULL UNIQUE,
    content     TEXT     NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    CONSTRAINT fk_report_campaign FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN (campaign_id) ON DELETE CASCADE
);

-- 사용 보고의 지출 항목 (합계는 모금액을 초과할 수 없음 — 서비스에서 검증)
CREATE TABLE REPORT_EXPENSE (
    expense_id BIGINT       NOT NULL AUTO_INCREMENT,
    report_id  BIGINT       NOT NULL,
    item       VARCHAR(200) NOT NULL,
    amount     BIGINT       NOT NULL,
    PRIMARY KEY (expense_id),
    CONSTRAINT fk_expense_report FOREIGN KEY (report_id) REFERENCES CAMPAIGN_REPORT (report_id) ON DELETE CASCADE
);

-- 보고서 첨부 사진 (영수증 / 후기, 캠페인당 여러 장)
CREATE TABLE REPORT_IMAGE (
    image_id   BIGINT       NOT NULL AUTO_INCREMENT,
    report_id  BIGINT       NOT NULL,
    image_url  VARCHAR(300) NOT NULL COMMENT '이미지 경로 (/uploads/...)',
    image_type ENUM('RECEIPT', 'REVIEW') NOT NULL COMMENT 'RECEIPT=영수증, REVIEW=후기',
    PRIMARY KEY (image_id),
    CONSTRAINT fk_report_image FOREIGN KEY (report_id) REFERENCES CAMPAIGN_REPORT (report_id) ON DELETE CASCADE
);
