CREATE TABLE `tasting_tag`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `kor_name`       VARCHAR(255) NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL,
    `icon`           VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    `last_modify_by` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

INSERT INTO `tasting_tag` (`kor_name`, `eng_name`, `icon`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES
    ('초콜릿', 'Chocolate', NULL, NULL, NULL, NULL, NULL),
    ('시트러스', 'Citric', NULL, NULL, NULL, NULL, NULL),
    ('석탄가스', 'Coal-gas', NULL, NULL, NULL, NULL, NULL),
    ('조리된 과일', 'Cooked Fruit', NULL, NULL, NULL, NULL, NULL),
    ('조리된 곡물', 'Cooked Mash', NULL, NULL, NULL, NULL, NULL),
    ('조리된 채소', 'Cooked Vegetable', NULL, NULL, NULL, NULL, NULL),
    ('건과일', 'Dried Fruit', NULL, NULL, NULL, NULL, NULL),
    ('향긋한', 'Fragrant', NULL, NULL, NULL, NULL, NULL),
    ('신선한 과일', 'Fresh Fruit', NULL, NULL, NULL, NULL, NULL),
    ('온실향', 'Green-House', NULL, NULL, NULL, NULL, NULL),
    ('건초', 'Hay-like', NULL, NULL, NULL, NULL, NULL),
    ('꿀', 'Honey', NULL, NULL, NULL, NULL, NULL),
    ('껍질', 'Husky', NULL, NULL, NULL, NULL, NULL),
    ('훈제 청어', 'Kippery', NULL, NULL, NULL, NULL, NULL),
    ('잎사귀', 'Leafy', NULL, NULL, NULL, NULL, NULL),
    ('가죽', 'Leathery', NULL, NULL, NULL, NULL, NULL),
    ('맥아 추출물', 'Malt Extract', NULL, NULL, NULL, NULL, NULL),
    ('의료', 'Medicinal', NULL, NULL, NULL, NULL, NULL),
    ('이끼', 'Mossy', NULL, NULL, NULL, NULL, NULL),
    ('새 나무', 'New Wood', NULL, NULL, NULL, NULL, NULL),
    ('견과류', 'Nutty', NULL, NULL, NULL, NULL, NULL),
    ('오래된 나무', 'Old Wood', NULL, NULL, NULL, NULL, NULL),
    ('플라스틱', 'Plastic', NULL, NULL, NULL, NULL, NULL),
    ('고무', 'Rubbery', NULL, NULL, NULL, NULL, NULL),
    ('모래', 'Sandy', NULL, NULL, NULL, NULL, NULL),
    ('셰리', 'Sherried', NULL, NULL, NULL, NULL, NULL),
    ('용제', 'Solvent', NULL, NULL, NULL, NULL, NULL),
    ('구운', 'Toasted', NULL, NULL, NULL, NULL, NULL),
    ('담배', 'Tobacco', NULL, NULL, NULL, NULL, NULL),
    ('바닐라', 'Vanilla', NULL, NULL, NULL, NULL, NULL),
    ('식물성', 'Vegetative', NULL, NULL, NULL, NULL, NULL),
    ('효모', 'Yeasty', NULL, NULL, NULL, NULL, NULL),
    ('기름진', 'Oily', NULL, NULL, NULL, NULL, NULL),
    ('훈연', 'Smokey', NULL, NULL, NULL, NULL, NULL),
    ('땀냄새', 'Sweaty', NULL, NULL, NULL, NULL, NULL);



CREATE TABLE `alcohols_tasting_tags`(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `alcohols_id`    bigint NOT NULL,
    `tasting_tag_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
);

INSERT INTO `alcohols_tasting_tags` (`alcohols_id`, `tasting_tag_id`)
VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
    (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16),
    (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24),
    (1, 25), (1, 26), (1, 27), (1, 28), (1, 29), (1, 30), (1, 31), (1, 32),
    (1, 33), (1, 34), (1, 35);

INSERT INTO `alcohols_tasting_tags` (`alcohols_id`, `tasting_tag_id`)
VALUES
    (2, 35), (2, 16), (2, 7), (2, 17), (2, 28), (2, 1), (2, 3),
    (2, 12), (2, 19), (2, 21), (2, 25), (2, 2), (2, 4), (2, 18),
    (2, 23), (2, 26), (2, 8), (2, 14), (2, 22), (2, 29),
    (2, 30), (2, 31), (2, 32), (2, 5), (2, 6), (2, 9),
    (2, 10), (2, 11), (2, 13), (2, 15), (2, 20), (2, 24),
    (2, 27);
INSERT INTO `alcohols_tasting_tags` (`alcohols_id`, `tasting_tag_id`)
VALUES
    (3, 31), (3, 9), (3, 11), (3, 12), (3, 35),
    (3, 30), (3, 1), (3, 2), (3, 3), (3, 4),
    (3, 5), (3, 6), (3, 7), (3, 8), (3, 10),
    (3, 13), (3, 14), (3, 15), (3, 16), (3, 17),
    (3, 18), (3, 19), (3, 20), (3, 21), (3, 22),
    (3, 23), (3, 24), (3, 25), (3, 26), (3, 27),
    (3, 28), (3, 29), (3, 32);
INSERT INTO `alcohols_tasting_tags` (`alcohols_id`, `tasting_tag_id`)
VALUES
    (4, 12), (4, 9), (4, 1), (4, 16), (4, 31),
    (4, 2), (4, 19), (4, 4), (4, 21), (4, 11),
    (4, 6), (4, 3), (4, 5), (4, 7), (4, 8),
    (4, 10), (4, 13), (4, 14), (4, 15), (4, 17),
    (4, 18), (4, 20), (4, 22), (4, 23), (4, 24),
    (4, 25), (4, 26), (4, 27), (4, 28), (4, 29),
    (4, 30), (4, 32);

INSERT INTO `alcohols_tasting_tags` (`alcohols_id`, `tasting_tag_id`)
VALUES (5, 2), (5, 9), (5, 12), (5, 17), (5, 26), (5, 1), (5, 8), (5, 19),
       (5, 25), (5, 28), (5, 30), (5, 27), (5, 3), (5, 4), (5, 5), (5, 6),
       (5, 7), (5, 10), (5, 11), (5, 13), (5, 14), (5, 15), (5, 16), (5, 18),
       (5, 20), (5, 21), (5, 22), (5, 23), (5, 24), (5, 29), (5, 31),
       (5, 32), (5, 33);





