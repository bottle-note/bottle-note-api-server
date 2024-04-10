CREATE TABLE `tasting_tag`
(
    `id`             bigint       NOT NULL,
    `kor_name`       VARCHAR(255) NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL,
    `icon`           VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE `alcohols_tasting_tags`(
    `id`             bigint NOT NULL,
    `alcohols_id`    bigint NOT NULL,
    `tasting_tag_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
);
