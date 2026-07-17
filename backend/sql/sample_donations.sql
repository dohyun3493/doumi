-- ============================================================
-- 대시보드 '최근 6개월 기부 내역' 막대그래프 확인용 테스트 데이터
--
-- - member_id / campaign_id 는 기존에 존재하는 첫 번째 행을 자동으로 사용한다.
--   (DONATION 은 member_id, campaign_id 가 FK 라서 실제 존재하는 값이어야 한다.)
-- - 최근 6개월 각 달 15일에 기부 1건씩 넣는다. (금액은 막대 높이 차이를 보려고 다르게)
-- - 실행 후 관리자 대시보드를 새로고침하면 6개 막대가 채워진다.
--
-- 주의: 이 데이터는 '총 누적 기부금' 같은 다른 통계에도 합산된다(테스트용).
-- ============================================================

SET @mid = (SELECT member_id   FROM MEMBER   ORDER BY member_id   LIMIT 1);
SET @cid = (SELECT campaign_id FROM CAMPAIGN ORDER BY campaign_id LIMIT 1);

INSERT INTO DONATION (member_id, campaign_id, amount, donated_at) VALUES
(@mid, @cid, 120000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-15')),
(@mid, @cid,  80000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 MONTH), '%Y-%m-15')),
(@mid, @cid, 250000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 3 MONTH), '%Y-%m-15')),
(@mid, @cid, 150000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 MONTH), '%Y-%m-15')),
(@mid, @cid, 300000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-15')),
(@mid, @cid, 200000, CURDATE());

-- 같은 달에 여러 건이 쌓이는 것(SUM 집계)도 보고 싶으면 아래를 추가 실행:
-- INSERT INTO DONATION (member_id, campaign_id, amount, donated_at) VALUES
-- (@mid, @cid, 50000, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-20'));

-- ------------------------------------------------------------
-- [되돌리기] 위에서 넣은 테스트 데이터만 지우려면, 넣기 직전의 MAX(donation_id)를
-- 기억해 두고 그보다 큰 것을 지우는 것이 가장 안전하다. 예:
--   SELECT MAX(donation_id) FROM DONATION;   -- (넣기 전에 값 확인 → 예: 1000)
--   DELETE FROM DONATION WHERE donation_id > 1000;   -- (넣은 뒤 실행)
-- ------------------------------------------------------------
