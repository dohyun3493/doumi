-- ============================================================
--  랭킹 데모용 더미 데이터  (2026-01 ~ 2026-05)
--  - 데모 기부자 8명 + 월별 기부내역(donation) 생성
--  - ranking 테이블을 배치와 "동일한 집계 로직"(DENSE_RANK)으로 채움
--    → 랭킹 페이지에서 1~5월이 바로 보임
--  - 여러 번 실행해도 안전(idempotent): 데모 데이터를 먼저 정리하고 다시 생성
--
--  [EC2 실행법]  ~/donation 에서:
--    cat backend/sql/demo_ranking_seed.sql \
--      | sudo docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4'
-- ============================================================
USE donation;

-- 0) 기부 대상 캠페인 1개 선택 (가장 첫 캠페인) — FK 충족용
SET @cid = (SELECT campaign_id FROM campaign ORDER BY campaign_id LIMIT 1);

-- 1) 재실행 안전: 이전 데모 기부내역 제거
DELETE d FROM donation d
  JOIN member m ON d.member_id = m.member_id
 WHERE m.email LIKE 'demo_donor%@demo.local';

-- 2) 데모 기부자 8명 (이미 있으면 무시).
--    password는 자리표시자 — 이 계정으로 로그인하지는 않음(랭킹 표시에만 사용).
INSERT IGNORE INTO member (email, password, name, member_type, point_balance, created_at) VALUES
 ('demo_donor1@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '김도윤', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor2@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '이서연', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor3@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '박지호', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor4@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '최하은', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor5@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '정우진', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor6@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '강민서', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor7@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '윤서아', 'INDIVIDUAL', 0, NOW()),
 ('demo_donor8@demo.local', '$2a$10$demoDemoDemoDemoDemoDeOJ2demoDemoDemoDemoDemoDemoDe', '한지안', 'INDIVIDUAL', 0, NOW());

-- 3) 기부내역 생성: 8명 × 5개월. 월별 보너스로 총액이 달라져 매월 순위표가 채워짐.
--    amount = (기부자 기본액) + (월별 보너스)
INSERT INTO donation (member_id, campaign_id, amount, donated_at)
SELECT m.member_id,
       @cid,
       d.base + mo.bonus,
       CONCAT('2026-', mo.mm, '-15 12:00:00')
FROM (
        SELECT 'demo_donor1@demo.local' AS email, 600000 AS base UNION ALL
        SELECT 'demo_donor2@demo.local', 540000 UNION ALL
        SELECT 'demo_donor3@demo.local', 480000 UNION ALL
        SELECT 'demo_donor4@demo.local', 430000 UNION ALL
        SELECT 'demo_donor5@demo.local', 390000 UNION ALL
        SELECT 'demo_donor6@demo.local', 350000 UNION ALL
        SELECT 'demo_donor7@demo.local', 300000 UNION ALL
        SELECT 'demo_donor8@demo.local', 260000
     ) d
JOIN member m ON m.email = d.email
CROSS JOIN (
        SELECT '01' AS mm,      0 AS bonus UNION ALL
        SELECT '02',       50000 UNION ALL
        SELECT '03',      -20000 UNION ALL
        SELECT '04',      120000 UNION ALL
        SELECT '05',       30000
     ) mo;

-- 4) ranking 테이블 재집계 (배치와 동일: 월별 DENSE_RANK, 음수/0 제외).
--    기존 1~5월 랭킹을 비우고 donation 으로부터 다시 계산한다.
DELETE FROM ranking
 WHERE `year_month` IN ('2026-01','2026-02','2026-03','2026-04','2026-05');

INSERT INTO ranking (member_id, `year_month`, total_amount, `rank`)
SELECT member_id, ym, total_amount,
       DENSE_RANK() OVER (PARTITION BY ym ORDER BY total_amount DESC) AS `rank`
FROM (
        SELECT d.member_id,
               DATE_FORMAT(d.donated_at, '%Y-%m') AS ym,
               SUM(d.amount)                      AS total_amount
        FROM donation d
        WHERE d.donated_at >= '2026-01-01'
          AND d.donated_at <  '2026-06-01'
        GROUP BY d.member_id, DATE_FORMAT(d.donated_at, '%Y-%m')
     ) t
WHERE total_amount > 0;

-- 5) 확인용 출력
SELECT `year_month`, COUNT(*) AS donors, MAX(total_amount) AS top_amount
FROM ranking
WHERE `year_month` BETWEEN '2026-01' AND '2026-05'
GROUP BY `year_month`
ORDER BY `year_month`;
