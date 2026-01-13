-- 조회수 기반 인기 주류 테스트 데이터
-- 이번 주 기준으로 조회 기록 생성

-- alcohol_id 1: 5명 조회 (가장 인기)
INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at)
VALUES (1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (3, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (5, 1, DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE view_at = VALUES(view_at);

-- alcohol_id 2: 4명 조회
INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at)
VALUES (1, 2, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (2, 2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (3, 2, DATE_SUB(NOW(), INTERVAL 3 DAY)),
       (4, 2, DATE_SUB(NOW(), INTERVAL 4 DAY))
ON DUPLICATE KEY UPDATE view_at = VALUES(view_at);

-- alcohol_id 3: 3명 조회
INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at)
VALUES (1, 3, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (2, 3, DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (3, 3, DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE view_at = VALUES(view_at);

-- alcohol_id 4: 2명 조회
INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at)
VALUES (1, 4, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       (2, 4, DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE view_at = VALUES(view_at);

-- alcohol_id 5: 1명 조회
INSERT INTO alcohols_view_histories (user_id, alcohol_id, view_at)
VALUES (1, 5, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE view_at = VALUES(view_at);
