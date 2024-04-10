
CREATE TABLE `notice`
(
    `id`             bigint       NOT NULL,
    `title`          VARCHAR(255) NULL,
    `category`       VARCHAR(255) NULL,
    `content`        VARCHAR(255) NULL,
    `view_count`     bigint NULL,
    `admin_id`       bigint NULL COMMENT '추후 어드민 역할 추가 후',
    `last_modify_at` TIMESTAMP    NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);



CREATE TABLE `review_report`
(
    `id`               bigint       NOT NULL,
    `user_id`          bigint       NOT NULL COMMENT '신고자',
    `review_id`        bigint       NOT NULL COMMENT '신고 대상 리뷰',
    `type`             VARCHAR(255) NOT NULL COMMENT 'user, review 등 신고의 타입',
    `report_content`   VARCHAR(255) NOT NULL COMMENT '어떤 문제로 신고했는지.',
    `status`           VARCHAR(255) NOT NULL DEFAULT 'waiting' COMMENT '진행상태',
    `admin_id`         bigint       NULL COMMENT '처리 어드민',
    `response_content` VARCHAR(255) NULL COMMENT '처리 결과',
    `last_modify_at`   TIMESTAMP    NOT NULL,
    `last_modify_by`   VARCHAR(255) NOT NULL,
    `create_at`        TIMESTAMP    NOT NULL,
    `create_by`        VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `user_report`
(
    `id`               bigint       NOT NULL,
    `user_id`          bigint       NOT NULL COMMENT '신고자',
    `report_user`      bigint       NOT NULL COMMENT '신고 대상자',
    `type`             VARCHAR(255) NOT NULL COMMENT 'user , review 등 신고의 타입',
    `content`          VARCHAR(255) NOT NULL COMMENT '어떤 문제로 신고했는지.',
    `status`           VARCHAR(255) NOT NULL DEFAULT 'waiting' COMMENT '진행상태',
    `admin_id`         bigint NULL COMMENT '처리 어드민',
    `response_content` VARCHAR(255) NULL COMMENT '처리 결과',
    `last_modify_at`   TIMESTAMP    NOT NULL,
    `last_modify_by`   VARCHAR(255) NOT NULL,
    `create_at`        TIMESTAMP    NOT NULL,
    `create_by`        VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `help`
(
    `id`               bigint       NOT NULL,
    `user_id`          bigint       NOT NULL COMMENT '문의자',
    `type`             VARCHAR(255) NOT NULL COMMENT 'add , user , etc',
    `title`            VARCHAR(255) NOT NULL COMMENT '문의 제목',
    `content`          VARCHAR(255) NOT NULL COMMENT '어떤 문제를 문의했는지.',
    `status`           VARCHAR(255) NOT NULL DEFAULT 'waiting' COMMENT '진행상태',
    `admin_id`         bigint NULL COMMENT '처리 어드민',
    `response_content` VARCHAR(255) NULL COMMENT '처리 결과',
    `last_modify_at`   TIMESTAMP    NOT NULL,
    `last_modify_by`   VARCHAR(255) NOT NULL,
    `create_at`        TIMESTAMP    NOT NULL,
    `create_by`        VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);


