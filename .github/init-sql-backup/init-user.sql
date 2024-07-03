CREATE TABLE `users`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `email`          VARCHAR(255) NOT NULL COMMENT '사용자 이메일',
    `nick_name`      VARCHAR(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            integer      NULL COMMENT '사용자 나이',
    `image_url`      VARCHAR(255) NULL COMMENT '사용자 프로필 이미지',
    `image_key`      VARCHAR(255) NULL COMMENT '업로드된 루트 경로(버킷부터 이미지 이름까지)',
    `image_path`     VARCHAR(255) NULL COMMENT '저장된 이미지 경로(버킷부터 최종폴더까지)',
    `gender`         VARCHAR(255) NULL,
    `role`           VARCHAR(255) NOT NULL DEFAULT 'GUEST',
    `social_type`    VARCHAR(255) NOT NULL COMMENT '소셜 타입 (NAVER, GOOGLE, APPLE)',
    `refresh_token`  VARCHAR(255) NULL COMMENT 'access token 재발급을 위한 토큰',
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `picks`(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `user_id`        bigint       NOT NULL,
    `alcohols_id`     bigint      NOT NULL,
    `status`         VARCHAR(255) NOT NULL COMMENT '찜 취소 , 찜 재 취소',
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    PRIMARY KEY (`id`)
);



CREATE TABLE `follow`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `user_id`        bigint       NOT NULL,
    `follow_user_id` bigint       NOT NULL COMMENT '팔로우 할 대상 아이디',
    `last_modify_at` TIMESTAMP    NULL,
    `last_modify_by` VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);
