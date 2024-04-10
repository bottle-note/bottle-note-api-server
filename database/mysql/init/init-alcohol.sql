CREATE TABLE `alcohol`(
    `id`             bigint       NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL COMMENT '영문 이름',
    `kor_name`       VARCHAR(255) NOT NULL COMMENT '한글 이름',
    `abv`            VARCHAR(255) NOT NULL COMMENT '도수',
    `category`       VARCHAR(255) NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리',
    `type`           VARCHAR(255) NOT NULL COMMENT '위스키, 럼, 브랜디, 진, 보드카, 데킬라, 기타',
    `country_id`     BIGINT NOT NULL COMMENT 'https://www.data.go.kr/data/15076566/fileData.do?recommendDataYn=Y',
    `distillery_id`  BIGINT NOT NULL COMMENT '증류소 정보',
    `tier_id`        VARCHAR(255) NOT NULL COMMENT '등급 정보',
    `cask`           VARCHAR(255) NULL COMMENT '캐스트 타입(단순 문자열로 박기)',
    `create_at`      TIMESTAMP    NOT NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `create_by`      VARCHAR(255) NOT NULL,
    `last_modify_by` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE `tier`
(
    `id`             bigint       NOT NULL,
    `type`           VARCHAR(255) NOT NULL COMMENT '위스키 / 꼬냑 / 진',
    `tier`           VARCHAR(255) NOT NULL COMMENT '등급명',
    `last_modify_at` TIMESTAMP    NOT NULL,
    `last_modify_by` TIMESTAMP    NOT NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `create_by`      TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE `country`
(
    `id`             bigint       NOT NULL COMMENT 'https://www.data.go.kr/data/15076566/fileData.do?recommendDataYn=Y',
    `country_kor_nm` VARCHAR(255) NOT NULL,
    `country_eng_nm` VARCHAR(255) NOT NULL,
    `iso_alpha2`     VARCHAR(255) NULL,
    `iso_alpha3`     VARCHAR(255) NULL,
    `iso_number`     VARCHAR(255) NULL,
    `continent`      VARCHAR(255) NULL COMMENT '대륙',
    PRIMARY KEY (`id`)
);

CREATE TABLE `distillery`
(
    `id`             bigint       NOT NULL,
    `kor_name`       VARCHAR(255) NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL,
    `logo_img_path`  VARCHAR(255) NULL,
    `last_modify_at` TIMESTAMP    NOT NULL,
    `last_modify_by` TIMESTAMP    NOT NULL,
    `create_at`      TIMESTAMP    NOT NULL,
    `create_by`      TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
);
