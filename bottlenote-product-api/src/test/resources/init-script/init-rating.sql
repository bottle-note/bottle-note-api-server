-- 평점 테스트 데이터 (부족분 채우기용)

-- alcohol 6~10: 평점 높은 순 (조회수 데이터 없음 -> 평점 기반으로 채워질 대상)
INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 6, 5.0, NOW(), NOW()),
       (2, 6, 4.5, NOW(), NOW()),
       (3, 6, 5.0, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 7, 4.5, NOW(), NOW()),
       (2, 7, 4.5, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 8, 4.0, NOW(), NOW()),
       (2, 8, 4.0, NOW(), NOW()),
       (3, 8, 4.5, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 9, 3.5, NOW(), NOW()),
       (2, 9, 4.0, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 10, 3.0, NOW(), NOW()),
       (2, 10, 3.5, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

-- alcohol 1~5: 조회수 있는 주류들의 평점
INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 1, 4.0, NOW(), NOW()),
       (2, 1, 4.5, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 2, 3.5, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);

INSERT INTO ratings (user_id, alcohol_id, rating, create_at, last_modify_at)
VALUES (1, 3, 4.0, NOW(), NOW())
ON DUPLICATE KEY UPDATE rating = VALUES(rating);
