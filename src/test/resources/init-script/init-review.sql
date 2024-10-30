INSERT INTO review
(id, user_id, alcohol_id, is_best, content, size_type, price, location_name, zip_code, address,
 detail_address, category, map_url, latitude, longitude, status,
 image_url, view_count, active_status, create_at, create_by, last_modify_at, last_modify_by)
VALUES (1, 2, 1, true, '이 위스키는 풍부하고 복잡한 맛이 매력적입니다.', 'BOTTLE', 65000, 'xxPub', '06000',
        '서울시 강남구 청담동', 'xxPub 청담점', 'bar', 'https://maps.example.com/map1', '12.123', '12.123',
        'PUBLIC', 'https://example.com/image01.jpg', NULL, 'ACTIVE', '2024-05-05 12:00:00', NULL,
        NULL, NULL),
       (2, 2, 1, false, '가벼우면서도 깊은 맛이 느껴지는 위스키입니다.', 'GLASS', 45000, 'xxPub', '06000',
        '서울시 강남구 청담동', 'xxPub 청담점', 'bar', 'https://maps.example.com/map2', '12.123', '12.123',
        'PUBLIC', 'https://example.com/image02.jpg', NULL, 'ACTIVE', '2024-05-02 13:00:00', NULL,
        NULL, NULL),
       (3, 2, 1, false, '향기로운 바닐라 향이 나는 부드러운 위스키입니다.', 'BOTTLE', 77000, 'xxPub', '06000',
        '서울시 강남구 청담동', 'xxPub 청담점', 'bar', 'https://maps.example.com/map3', '12.123', '12.123',
        'PUBLIC', 'https://example.com/image03.jpg', NULL, 'ACTIVE', '2024-05-16 14:30:00', NULL,
        NULL, NULL),
       (4, 2, 4, false, '스모키하고 강한 페트 향이 인상적인 위스키입니다.', 'BOTTLE', 120000, 'xxPub', '06000',
        '서울시 강남구 청담동', 'xxPub 청담점', 'bar', 'https://maps.example.com/map4', '12.123', '12.123',
        'PUBLIC', 'https://example.com/image04.jpg', NULL, 'ACTIVE', '2024-05-01 15:45:00', NULL,
        NULL, NULL),
       (5, 2, 2, false, '달콤한 캐러멜과 과일 향이 조화를 이루는 맛있습니다.', 'GLASS', 99000, 'xxPub', '06000',
        '서울시 강남구 청담동', 'xxPub 청담점', 'bar', 'https://maps.example.com/map5', '12.123', '12.123',
        'PUBLIC', 'https://example.com/image05.jpg', NULL, 'ACTIVE', '2024-05-08 16:00:00', NULL,
        NULL, NULL);