CREATE TABLE `users`
(
    `id`             bigint       NOT NULL,
    `email`          VARCHAR(255) NOT NULL COMMENT '사용자 이메일',
    `nick_name`      VARCHAR(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            VARCHAR(255) NULL COMMENT '사용자 나이',
    `image_url`      VARCHAR(255) NULL COMMENT '사용자 프로필 이미지',
    `gender`         VARCHAR(255) NULL,
    `role`           VARCHAR(255) NOT NULL DEFAULT 'GUEST',
    `social_type`    VARCHAR(255) NOT NULL COMMENT '소셜 타입 (NAVER, GOOGLE, APPLE)',
    `refresh_token`  VARCHAR(255) NOT NULL COMMENT 'access token 재발급을 위한 토큰',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `picks`(
    `id`             bigint       NOT NULL,
    `user_id`        bigint       NOT NULL,
    `alcohos_id`     bigint       NOT NULL,
    `status`         VARCHAR(255) NOT NULL COMMENT '찜 취소 , 찜 재 취소',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
);



CREATE TABLE `follow`
(
    `id`             bigint       NOT NULL,
    `user_id`        VARCHAR(255) NOT NULL,
    `follow_user_id` VARCHAR(255) NOT NULL COMMENT '팔로우 할 대상 아이디',
    `last_modify_at` TIMESTAMP    NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);
