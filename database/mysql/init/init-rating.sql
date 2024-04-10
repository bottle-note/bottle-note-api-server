CREATE TABLE `rating`
(
    `id`             bigint       NOT NULL,
    `alcohol_id`     bigint       NOT NULL,
    `user_id`        bigint       NOT NULL,
    `rating`         VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);
