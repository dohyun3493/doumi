-- 결제 원장에 '결과를 모르는' 상태를 추가하는 마이그레이션
--
-- 배경: 토스 승인 호출이 DB 트랜잭션 안에 있어, 승인 직후 장애가 나면 트랜잭션이 롤백되고
--       토스에는 승인만 남아 "돈은 빠졌는데 포인트는 없는" 건이 생겼다. 기존 status에는
--       CHARGED/CANCELED뿐이라 이 '모름' 구간을 표현할 수 없어 복구가 불가능했다.
--
-- 변경: 승인 전에 PENDING을 먼저 커밋하고, 결과를 모른 채 중단되면 그 흔적을 근거로
--       스케줄러가 토스에 재조회해 확정(전진 복구)한다.

ALTER TABLE payment
    MODIFY status ENUM('PENDING', 'CHARGED', 'CANCELING', 'CANCELED', 'FAILED')
        NOT NULL DEFAULT 'PENDING';

-- 방치된 건(PENDING/CANCELING)을 찾는 기준. 진행 중인 정상 요청까지 건드리지 않도록
-- 마지막 상태 변경으로부터 일정 시간이 지난 건만 대사 대상으로 삼는다.
ALTER TABLE payment
    ADD COLUMN updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- 대사 스케줄러의 조회 성능 확보
CREATE INDEX idx_payment_status_updated ON payment (status, updated_at);
