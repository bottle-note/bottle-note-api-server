CREATE TABLE `review`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `user_id`        bigint       NOT NULL,
    `alcohols_id`    bigint       NOT NULL,
    `content`        VARCHAR(255) NOT NULL,
    `size_type`      VARCHAR(255) NULL COMMENT '잔 : glass, 보틀 : bottle',
    `price`          bigint       NULL,
    `zip_code`       VARCHAR(255) NULL,
    `address`        VARCHAR(255) NULL,
    `detail_address` VARCHAR(255) NULL,
    `image_url`      VARCHAR(255) NULL COMMENT '썸네일 이미지',
    `view_count`     bigint       NULL COMMENT '조회수',
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    `last_modify_by` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `review_image`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `review_id`      bigint       NOT NULL,
    `image_url`      VARCHAR(255) NOT NULL,
    `file_name`      VARCHAR(255) NOT NULL,
    `file_size`      VARCHAR(255) NOT NULL,
    `image_order`    bigint       NOT NULL,
    `status`         VARCHAR(255) NULL COMMENT '삭제됨 / 숨김처리됨 / 유효기간이 만료됨 등등',
    `tags`           VARCHAR(255) NULL,
    `description`    VARCHAR(255) NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `last_modify_by` VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `review_reply`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `review_id`       bigint       NOT NULL,
    `user_id`         bigint       NOT NULL,
    `parent_reply_id` bigint       NULL,
    `content`         VARCHAR(255) NOT NULL,
    `last_modify_at`  TIMESTAMP    NULL,
    `last_modify_by`  VARCHAR(255) NULL,
    `create_at`       TIMESTAMP    NULL,
    `create_by`       VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);
