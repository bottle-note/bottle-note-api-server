CREATE TABLE `alcohol`(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `eng_name`       VARCHAR(255) NOT NULL COMMENT '영문 이름',
    `kor_name`       VARCHAR(255) NOT NULL COMMENT '한글 이름',
    `abv`            VARCHAR(255) NOT NULL COMMENT '도수',
    `category`       VARCHAR(255) NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리',
    `type`           VARCHAR(255) NOT NULL COMMENT '위스키, 럼, 브랜디, 진, 보드카, 데킬라, 기타',
    `country_id`     BIGINT NOT NULL COMMENT 'https://www.data.go.kr/data/15076566/fileData.do?recommendDataYn=Y',
    `distillery_id`  BIGINT NOT NULL COMMENT '증류소 정보',
    `tier_id`        BIGINT NOT NULL COMMENT '등급 정보',
    `cask`           VARCHAR(255) NULL COMMENT '캐스트 타입(단순 문자열로 박기)',
    `create_at`      TIMESTAMP    NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    `last_modify_by` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

-- 첫 번째 행 데이터 삽입
INSERT INTO `alcohol` (`eng_name`, `kor_name`, `abv`, `category`, `type`, `country_id`, `distillery_id`, `tier_id`, `cask`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES ('1770 Glasgow Single Malt', '1770 글래스고 싱글 몰트', '46.0 % Vol.', 'Marriage of Ex-Bourbon & Virgin Oak Casks', 'Triple Distilled', 1, 1, 1, 'Marriage of Ex-Bourbon & Virgin Oak Casks', NULL, NULL, NULL, NULL);

-- 두 번째 행 데이터 삽입
INSERT INTO `alcohol` (`eng_name`, `kor_name`, `abv`, `category`, `type`, `country_id`, `distillery_id`, `tier_id`, `cask`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES ('1770 Glasgow Single Malt', '1770 글래스고 싱글 몰트', '46.0 % Vol.', 'Virgin Oak & PX Sherry Cask Finish', 'Peated', 1, 1, 2, 'Virgin Oak & PX Sherry Cask Finish', NULL, NULL, NULL, NULL);

-- 세 번째 행 데이터 삽입
INSERT INTO `alcohol` (`eng_name`, `kor_name`, `abv`, `category`, `type`, `country_id`, `distillery_id`, `tier_id`, `cask`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES ('1770 Glasgow Single Malt', '1770 글래스고 싱글 몰트', '46.0 % Vol.', '1st fill ex-bourbon, finish virgin oak', 'The Original - Fresh & Fruity', 1, 1, 3, '1st fill ex-bourbon, finish virgin oak', NULL, NULL, NULL, NULL);

-- 네 번째 행 데이터 삽입
INSERT INTO `alcohol` (`eng_name`, `kor_name`, `abv`, `category`, `type`, `country_id`, `distillery_id`, `tier_id`, `cask`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES ('Aberfeldy 12-year-old', '애버펠디 12년', '40.0 % Vol.', NULL, 'Single Malt', 1, 2, 4, NULL, NULL, NULL, NULL, NULL);

-- 다섯 번째 행 데이터 삽입
INSERT INTO `alcohol` (`eng_name`, `kor_name`, `abv`, `category`, `type`, `country_id`, `distillery_id`, `tier_id`, `cask`, `create_at`, `last_modify_at`, `create_by`, `last_modify_by`)
VALUES ('Aberfeldy 16-year-old', '애버펠디 16년', '40.0 % Vol.', 'Bourbon, 6 month finish on first fill Oloroso', 'Single Malt', 1, 2, 5, 'Bourbon, 6 month finish on first fill Oloroso', NULL, NULL, NULL, NULL);


CREATE TABLE `tier`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `type`           VARCHAR(255) NOT NULL COMMENT '위스키 / 꼬냑 / 진',
    `tier`           VARCHAR(255) NULL COMMENT '등급명',
    `last_modify_at` TIMESTAMP    NULL,
    `last_modify_by` VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

INSERT INTO `tier` (`type`, `tier`, `last_modify_at`, `last_modify_by`, `create_at`, `create_by`)
VALUES     -- Whiskey types
           ('Whiskey', NULL, NULL, NULL, NULL, NULL),

           -- Cognac types
           ('Cognac', 'VS', NULL, NULL, NULL, NULL),
           ('Cognac', 'VSOP', NULL, NULL, NULL, NULL),
           ('Cognac', 'XO', NULL, NULL, NULL, NULL),
           ('Cognac', NULL, NULL, NULL, NULL, NULL),

           -- Gin types
           ('Gin', 'Standard', NULL, NULL, NULL, NULL),
           ('Gin', 'Premium', NULL, NULL, NULL, NULL),
           ('Gin', NULL, NULL, NULL, NULL, NULL);



CREATE TABLE `country`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT 'https://www.data.go.kr/data/15076566/fileData.do?recommendDataYn=Y',
    `kor_name`       VARCHAR(255) NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL,
    `iso_alpha2`     VARCHAR(255) NULL,
    `iso_alpha3`     VARCHAR(255) NULL,
    `iso_number`     VARCHAR(255) NULL,
    `continent`      VARCHAR(255) NULL COMMENT '대륙',
    PRIMARY KEY (`id`)
);

-- Inserting data into the 'country' table
INSERT INTO `country` (`kor_name`, `eng_name`, `iso_alpha2`, `iso_alpha3`, `iso_number`, `continent`)
VALUES
    ('스코틀랜드', 'Scotland', 'GB', 'GBR', '826', 'Europe'),
    ('프랑스', 'France', 'FR', 'FRA', '250', 'Europe'),
    ('인도', 'India', 'IN', 'IND', '356', 'Asia'),
    ('아일랜드', 'Ireland', 'IE', 'IRL', '372', 'Europe');

CREATE TABLE `distillery`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `kor_name`       VARCHAR(255) NOT NULL,
    `eng_name`       VARCHAR(255) NOT NULL,
    `logo_img_path`  VARCHAR(255) NULL,
    `last_modify_at` TIMESTAMP    NULL,
    `last_modify_by` VARCHAR(255) NULL,
    `create_at`      TIMESTAMP    NULL,
    `create_by`      VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
);

-- Inserting data into the 'distillery' table
INSERT INTO `distillery` (`kor_name`, `eng_name`, `logo_img_path`, `last_modify_at`, `last_modify_by`, `create_at`, `create_by`)
VALUES ('글래스고 증류소', 'The Glasgow Distillery Co.', NULL, NULL, NULL, NULL, NULL),
        ('아버펠디', 'Aberfeldy', NULL, NULL, NULL, NULL, NULL),
        ('아벌라워', 'Aberlour', NULL, NULL, NULL, NULL, NULL),
        ('아무르트', 'Amrut', NULL, NULL, NULL, NULL, NULL);
