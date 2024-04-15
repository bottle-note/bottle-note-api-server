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


CREATE TABLE `alcohols_tasting_tags`(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `alcohols_id`    bigint NOT NULL,
    `tasting_tag_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
);
