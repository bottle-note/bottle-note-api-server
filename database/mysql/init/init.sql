CREATE TABLE `rating`
(
    `id`             bigint       NOT NULL,
    `alcohol_id`     bigint       NOT NULL,
    `user_id`        bigint       NOT NULL,
    `rating`         VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `create_by`      TIMESTAMP    NOT NULL,
    `last_modify_by` TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE `review`
(
    `id`             bigint       NOT NULL,
    `user_id`        bigint       NOT NULL,
    `alcohols_id`    bigint       NOT NULL,
    `content`        VARCHAR(255) NOT NULL,
    `size_type`      VARCHAR(255) NULL COMMENT '잔 : glass, 보틀 : bottle',
    `price`          VARCHAR(255) NULL,
    `zip_code`       VARCHAR(255) NULL,
    `address`        VARCHAR(255) NULL,
    `detail_address` VARCHAR(255) NULL,
    `image_url`      VARCHAR(255) NULL COMMENT '썸네일 이미지',
    `view_count`     VARCHAR(255) NULL COMMENT '조회수',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `review_image`
(
    `id`             bigint       NOT NULL,
    `review_id`      bigint       NOT NULL,
    `image_url`      VARCHAR(255) NOT NULL,
    `file_name`      VARCHAR(255) NOT NULL,
    `file_size`      VARCHAR(255) NOT NULL,
    `order`          VARCHAR(255) NOT NULL,
    `status`         VARCHAR(255) NULL COMMENT '삭제됨 / 숨김처리됨 / 유효기간이 만료됨 등등',
    `tags`           VARCHAR(255) NULL,
    `description`    VARCHAR(255) NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `reply`
(
    `id`              bigint       NOT NULL,
    `review_id`       bigint       NOT NULL,
    `user_id`         bigint       NOT NULL,
    `parent_reply_id` bigint NULL,
    `last_modify_at`  TIMESTAMP    NOT NULL,
    `last_modify_by`  VARCHAR(255) NOT NULL,
    `create_at`       TIMESTAMP    NOT NULL,
    `create_by`       VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);
