CREATE TABLE `rating`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `alcohol_id`     bigint       NOT NULL,
    `user_id`        bigint       NOT NULL,
    `rating`         VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    `last_modify_by` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);
