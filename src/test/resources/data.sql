CREATE TABLE `region`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '국가',
    `kor_name`       varchar(255) NOT NULL COMMENT '국가 한글명',
    `eng_name`       varchar(255) NOT NULL COMMENT '국가 영문명',
    `continent`      varchar(255) NULL COMMENT '대륙',
    `description`    varchar(255) NULL COMMENT '주석',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`)
);
CREATE TABLE `distillery`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '증류소',
    `kor_name`       varchar(255) NOT NULL COMMENT '증류소 한글 이름',
    `eng_name`       varchar(255) NOT NULL COMMENT '증류소 영문 이름',
    `logo_img_url`   varchar(255) NULL COMMENT '로고 이미지',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`)
);
CREATE TABLE `alcohol`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '술',
    `kor_name`       varchar(255) NOT NULL COMMENT '한글 이름',
    `eng_name`       varchar(255) NOT NULL COMMENT '영문 이름',
    `abv`            varchar(255) NULL COMMENT '도수',
    `type`           varchar(255) NOT NULL COMMENT '위스키 고정 ( 추후 럼,진등으로 확장 가능)',
    `kor_category`   varchar(255) NOT NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리 한글명',
    `eng_category`   varchar(255) NOT NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리 영문명 ',
    `region_id`      bigint       NULL COMMENT 'https://www.data.go.kr/data/15076566/fileData.do?recommendDataYn=Y',
    `distillery_id`  bigint       NULL COMMENT '증류소 정보',
    `age`            varchar(255) NULL COMMENT '숙성년도',
    `cask`           varchar(255) NULL COMMENT '캐스트 타입(단순 문자열로 박기) - 한글 정제화가 힘들 수 있음. 영문사용 권장',
    `image_url`      varchar(255) NULL COMMENT '썸네일 이미지',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`region_id`) REFERENCES `region` (`id`),
    FOREIGN KEY (`distillery_id`) REFERENCES `distillery` (`id`)
);
CREATE TABLE IF NOT EXISTS `users`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '사용자',
    `email`          varchar(255) NOT NULL COMMENT '사용자 소셜 이메일',
    `nick_name`      varchar(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            int          NULL COMMENT '사용자 나이',
    `image_url`      varchar(255) NULL COMMENT '사용자 프로필 이미지',
    `gender`         varchar(255) NULL COMMENT '사용자 성별',
    `role`           varchar(255) NOT NULL DEFAULT 'GUEST' COMMENT '사용자 역할',
    `social_type`    varchar(255) NOT NULL COMMENT '소셜 타입 ( NAVER, GOOGLE, APPLE )',
    `refresh_token`  varchar(255) NULL COMMENT 'access token 재발급을 위한 토큰',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`)
);
CREATE TABLE `picks`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '찜하기',
    `user_id`        bigint       NOT NULL COMMENT '찜한 사용자',
    `alcohol_id`     bigint       NOT NULL COMMENT '찜한 술',
    `status`         varchar(255) NOT NULL COMMENT '찜 취소 찜 재취소',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
);
CREATE TABLE `user_report`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '유저 신고',
    `user_id`          bigint       NOT NULL COMMENT '신고자',
    `report_user_id`   bigint       NOT NULL COMMENT '신고 대상자',
    `type`             varchar(255) NOT NULL COMMENT '악성유저 ,스팸등 신고의 타입',
    `report_content`   varchar(255) NOT NULL COMMENT '어던 문제로 신고했는지.',
    `status`           varchar(255) NOT NULL DEFAULT 'WAITING' COMMENT '진행상태',
    `admin_id`         bigint       NULL COMMENT '처리  어드민',
    `response_content` varchar(255) NULL COMMENT '처리 결과',
    `create_at`        timestamp    NULL COMMENT '최초 생성일',
    `create_by`        varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at`   timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by`   varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`report_user_id`) REFERENCES `users` (`id`)
);
CREATE TABLE `rating`
(
    `alcohol_id`     bigint       NOT NULL COMMENT '평가 대상 술',
    `user_id`        bigint       NOT NULL COMMENT '평가자(사용자)',
    `rating`         DOUBLE       NOT NULL DEFAULT 0 COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`alcohol_id`, `user_id`)
);
CREATE TABLE `help`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '문의',
    `user_id`          bigint       NOT NULL COMMENT '문의자',
    `type`             varchar(255) NOT NULL COMMENT 'ADD , USER... 개발때 enum 추가',
    `title`            varchar(255) NOT NULL COMMENT '문의 제목',
    `help_content`     text         NOT NULL COMMENT '문의내용 최대 1000글자',
    `status`           varchar(255) NOT NULL DEFAULT 'WAITING' COMMENT '진행상태',
    `admin_id`         bigint       NULL COMMENT '처리  어드민',
    `response_content` varchar(255) NULL COMMENT 'WAITING : 대기중, SSUCCESS : 처리 완료 , REJECT : 반려',
    `create_at`        timestamp    NULL COMMENT '최초 생성일',
    `create_by`        varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at`   timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by`   varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);
CREATE TABLE `follow`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '팔로우',
    `user_id`        bigint       NOT NULL COMMENT '팔로우 하는 사람 아이디',
    `follow_user_id` bigint       NOT NULL COMMENT '팔로우 대상 아이디',
    `status`         varchar(255) NOT NULL COMMENT '팔로우, 언팔로우',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`follow_user_id`) REFERENCES `users` (`id`)
);
CREATE TABLE `tasting_tag`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '테이스팅 태그',
    `kor_name`       varchar(255) NOT NULL COMMENT '한글 태그 이름',
    `eng_name`       varchar(255) NOT NULL COMMENT '영문 태그 이름',
    `icon`           varchar(255) NULL COMMENT '앱 출시 후 디벨롭 할 때 사용',
    `description`    varchar(255) NULL COMMENT '태그 설명',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`)
);
CREATE TABLE `alcohol_tasting_tags`
(
    `id`             bigint    NOT NULL comment '술/테이스팅 태그 연관관계 해소',
    `alcohol_id`     bigint    NOT NULL comment '술 아이디',
    `tasting_tag_id` bigint    NOT NULL comment '태그 아이디',
    `create_at`      timestamp NULL comment '최초 생성일',
    `last_modify_at` timestamp NULL comment '최종 생성일',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`),
    FOREIGN KEY (`tasting_tag_id`) REFERENCES `tasting_tag` (`id`)
);
CREATE TABLE `review`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT COMMENT '술 리뷰',
    `user_id`        bigint         NOT NULL COMMENT '리뷰 작성자',
    `alcohol_id`     bigint         NOT NULL COMMENT '리뷰 대상 술',
    `content`        varchar(1000)  NOT NULL COMMENT '1000글자',
    `size_type`      varchar(255)   NULL COMMENT '잔 : GLASS , 보틀 : BOTTLE',
    `price`          DECIMAL(10, 2) NULL COMMENT '가격',
    `zip_code`       varchar(255)   NULL COMMENT '마신 장소 우편번호',
    `address`        varchar(255)   NULL COMMENT '마신 장소 주소',
    `detail_address` varchar(255)   NULL COMMENT '마신 장소 상세 주소',
    `status`         varchar(255)   NULL COMMENT '공개리뷰, 숨김리뷰',
    `image_url`      varchar(255)   NULL COMMENT '썸네일 이미지',
    `view_count`     bigint         NULL COMMENT '조회수',
    `active_status`  varchar(255)   NULL COMMENT '리뷰활성상태 (활성, 삭제, 비활성)',
    `create_at`      timestamp      NULL COMMENT '최초 생성일',
    `create_by`      varchar(255)   NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp      NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255)   NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
);
CREATE TABLE `review_report`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰 신고',
    `user_id`          bigint       NOT NULL COMMENT '신고자',
    `review_id`        bigint       NOT NULL COMMENT '신고 대상 리뷰',
    `type`             varchar(255) NOT NULL COMMENT '광고 리뷰인지, 욕설 리뷰인지등의 타입',
    `report_content`   varchar(255) NOT NULL COMMENT '어떤 문제로 신고했는지.',
    `status`           varchar(255) NOT NULL DEFAULT 'WAITING' COMMENT '진행상태',
    `admin_id`         bigint       NULL COMMENT '처리 어드민',
    `response_content` varchar(255) NULL COMMENT '처리 결과',
    `create_at`        timestamp    NULL COMMENT '최초 생성일',
    `create_by`        varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at`   timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by`   varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
);
CREATE TABLE `review_image`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰-이미지 등록은 최대 5장',
    `review_id`      bigint       NOT NULL comment '리뷰 아이디',
    `order`          bigint       NOT NULL COMMENT '이미지 순서',
    `image_url`      varchar(255) NOT NULL COMMENT 'S3 이미지 경로',
    `image_key`      varchar(255) NOT NULL COMMENT '업로드된 루트 경로(버킷부터 이미지 이름까지)',
    `image_path`     varchar(255) NOT NULL COMMENT '져장된 이미지의 경로(버킷부터 최종폴더까지)',
    `image_name`     varchar(255) NOT NULL COMMENT '생성된 UUID + 확장자 파일명',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
);
CREATE TABLE `review_tasting_tag`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '리뷰 테이스팅 태그 - 최대 10개',
    `review_id`      bigint      NOT NULL comment '리뷰 아이디',
    `tasting_tag`    varchar(12) NOT NULL COMMENT '테이스팅 태그 - 최대 12자',
    `create_at`      timestamp   NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp   NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
);
CREATE TABLE `review_reply`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰 댓글',
    `review_id`       bigint       NOT NULL COMMENT '리뷰 아이디',
    `user_id`         bigint       NOT NULL COMMENT '리뷰 작성자',
    `root_reply_id`   bigint       NULL comment '최상위 댓글 식별자',
    `parent_reply_id` bigint       NULL comment '상위 댓글 식별',
    `content`         text         NOT NULL COMMENT '댓글 최대 1000글자',
    `create_at`       timestamp    NULL COMMENT '최초 생성일',
    `create_by`       varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at`  timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by`  varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`parent_reply_id`) REFERENCES `review_reply` (`id`)
);
CREATE TABLE `notice`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '공지사항',
    `title`          varchar(255) NULL COMMENT '공지사항 제목',
    `category`       varchar(255) NULL COMMENT '공지사항 카테고리',
    `content`        text         NULL COMMENT '공지사항 내용 최대 1000',
    `view_count`     bigint       NULL COMMENT '조회수',
    `admin_id`       bigint       NULL COMMENT '추후 어드민 역할 추가 후',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`)
);
CREATE TABLE `likes`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '좋아요',
    `review_id`      bigint       NOT NULL COMMENT '좋아요의 대상 리뷰',
    `user_id`        bigint       NOT NULL COMMENT '좋아요를 누른 사람',
    `status`         varchar(255) NULL COMMENT '공감, 공감취소',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
);
CREATE TABLE `alcohol_image`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '술 이미지',
    `alcohol_id`     bigint       NOT NULL COMMENT '술 아이디',
    `order`          bigint       NOT NULL COMMENT '이미지 순서',
    `image_url`      varchar(255) NOT NULL COMMENT 'S3 이미지 경로',
    `image_key`      varchar(255) NOT NULL COMMENT '업로드된 루트 경로(버킷부터 이미지 이름까지)',
    `image_path`     varchar(255) NOT NULL COMMENT '져장된 이미지의 경로(버킷부터 최종폴더까지)',
    `image_name`     varchar(255) NOT NULL COMMENT '생성된 UUID + 확장자 파일명',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
);
CREATE TABLE `user_history`
(
    `id`             bigint       NOT NULL auto_increment COMMENT '히스토리 id',
    `user_id`        bigint       NOT NULL COMMENT '사용자 id',
    `alcohol_id`     bigint       NOT NULL COMMENT '알코올 id',
    `type`           varchar(255) NOT NULL COMMENT 'pick, review, rating',
    `action`         varchar(255) NULL COMMENT 'creat, update, delete',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
);


insert into region (kor_name, eng_name, continent, description, create_at, create_by, last_modify_at,
                    last_modify_by)
values ('호주', 'Australia', null, '오세아니아에 위치한 나라로 다양한 위스키를 생산.', '2024-06-04 17:19:39', 'admin', '2024-06-04 17:19:39',
        'admin'),
       ('핀란드', 'Finland', null, '북유럽에 위치한 나라로 청정한 자연환경을 자랑.', '2024-06-04 17:19:39', 'admin', '2024-06-04 17:19:39',
        'admin'),
       ('프랑스', 'France', null, '와인과 브랜디로 유명한 유럽의 나라.', '2024-06-04 17:19:39', 'admin', '2024-06-04 17:19:39', 'admin');

insert into distillery (kor_name, eng_name, logo_img_url, create_at, create_by, last_modify_at,
                        last_modify_by)
values ('글래스고', 'The Glasgow Distillery Co.', null, '2024-06-04 17:09:03', 'admin', '2024-06-04 17:09:03', 'admin'),
       ('글렌 그란트', 'Glen Grant', null, '2024-06-04 17:09:03', 'admin', '2024-06-04 17:09:03', 'admin'),
       ('글렌 기어리', 'Glen Garioch', null, '2024-06-04 17:09:03', 'admin', '2024-06-04 17:09:03', 'admin');

insert into alcohol (kor_name, eng_name, abv, type, kor_category, eng_category, region_id, distillery_id, age, cask,
                     image_url, create_at, create_by, last_modify_at, last_modify_by)
values ('라이터스 티얼즈 레드 헤드', 'Writers'' Tears Red Head', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 1, 3, null,
        'Oloroso Sherry Butts', 'https://static.whiskybase.com/storage/whiskies/1/8/3881/318643-big.jpg',
        '2024-06-08 05:06:00', 'admin', '2024-06-08 05:06:00', 'admin'),
       ('라이터스 티얼즈 더블 오크', 'Writers'' Tears Double Oak', '46', 'WHISKY', '블렌디드', 'Blend', 1, 2, null,
        'American & French Oak', 'https://static.whiskybase.com/storage/whiskies/1/3/1308/282645-big.jpg',
        '2024-06-08 05:06:00', 'admin', '2024-06-08 05:06:00', 'admin'),
       ('라이터스 티얼즈 코퍼 팟', 'Writers'' Tears Copper Pot', '40', 'WHISKY', '블렌디드 몰트', 'Blended Malt', 2, 1, null,
        'Bourbon Barrels', 'https://static.whiskybase.com/storage/whiskies/7/7/471/189958-big.jpg',
        '2024-06-08 05:06:00', 'admin', '2024-06-08 05:06:00', 'admin');

insert into users (email, nick_name, age, image_url, gender, role, social_type, refresh_token,
                   create_at, last_modify_at)
values ('hyejj19@naver.com', 'WOzU6J8541', null, null, 'null', 'ROLE_USER', 'KAKAO',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJoeWVqajE5QG5hdmVyLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjoxLCJpYXQiOjE3MTkwNDA2ODAsImV4cCI6MTcyMDI1MDI4MH0._s1r4Je9wFTvu_hV0sYBVRr5uDqiHXVBM22jS35YNbH0z-svrTYjysORA4J2J5GQcel9K5FxRBQnWjAeqQNfdw',
        null, null),
       ('chadongmin@naver.com', 'xIFo6J8726', null, null, 'null', 'ROLE_USER', 'KAKAO',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGFkb25nbWluQG5hdmVyLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjoyLCJpYXQiOjE3MjAzMzYzNzUsImV4cCI6MTcyMTU0NTk3NX0.HihJnS-hi2tP4f2i6OqcqDGeAiSUIiY_ExcGpANsiBVFHP17JKsRNTQhv5DjM-vmpC_Pir4bvUIXVGgMzYuDuA',
        null, null),
       ('dev.bottle-note@gmail.com', 'PARC6J8814', 25, null, 'MALE', 'ROLE_USER', 'GOOGLE',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkZXYuYm90dGxlLW5vdGVAZ21haWwuY29tIiwicm9sZXMiOiJST0xFX1VTRVIiLCJ1c2VySWQiOjMsImlhdCI6MTcyMDM1MDY4OCwiZXhwIjoxNzIxNTYwMjg4fQ.two8yLXv2xFCEOhuGrLYV8cmewm8bD8EbIWTXYa896MprhgclsGNDThspjRF9VJmSA3mxvoPjnBJ0ClneCClBQ',
        null, null),
       ('eva.park@oysterable.com', 'VOKs6J8831', null, null, 'null', 'ROLE_USER', 'GOOGLE',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJldmEucGFya0BveXN0ZXJhYmxlLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjo1LCJpYXQiOjE3MTc4MzU1MDUsImV4cCI6MTcxOTA0NTEwNX0.j6u6u8a8lhedeegOe2wqOjNZkMx0X3RgVeAcvnlCZmj_AXQF5WDo4k71WI-bFt_ypW-ewVCRmdLQoOduaggCRw',
        null, null),
       ('rlagusrl928@gmail.com', 'hpPw6J111837', null, null, 'MALE', 'ROLE_USER', 'GOOGLE',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJybGFndXNybDkyOEBnbWFpbC5jb20iLCJyb2xlcyI6IlJPTEVfVVNFUiIsInVzZXJJZCI6NiwiaWF0IjoxNzE4MDk4NjQxLCJleHAiOjE3MTkzMDgyNDF9.0nfUYMm4UEFzfE52ydulDZ0eX5U_2yBN4hCeBXr4PeA3xwbzDo7t2c2kJGNU_LXMbZg2Iz4DAIZnu0QB3DJ8VA',
        null, null),
       ('ytest@gmail.com', 'OMkS6J12123', null, null, 'null', 'ROLE_USER', 'GOOGLE',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ5dGVzdEBnbWFpbC5jb20iLCJyb2xlcyI6IlJPTEVfVVNFUiIsInVzZXJJZCI6NywiaWF0IjoxNzE4MTIzMDMxLCJleHAiOjE3MTkzMzI2MzF9.8KNJW6havezUSgaWmRyAvlxfwdRZxjdC7mcBuexN0Gy9NtgJIAVWqNMW0wlJXw7d9LVwtZf5Mv4aUdA_V-V8pw',
        null, null),
       ('juye@gmail.com', 'juye12', null, '{
  "viewUrl": "http://example.com/new-profile-image.jpg"
}', 'null', 'ROLE_USER', 'GOOGLE',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdXllQGdtYWlsLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjo4LCJpYXQiOjE3MjAyNTE5MjcsImV4cCI6MTcyMTQ2MTUyN30.bRs4junP0awvFqTYXTnjzxMxrUJDt2dh76BAcnj6xemWuGS26YZsOgrhGL0T-3JeYIh7dUkvjETWH1LB2N3Rjw',
        null, null),
       ('rkdtkfma@naver.com', 'iZBq6J22547', null, null, 'null', 'ROLE_USER', 'KAKAO',
        'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJya2R0a2ZtYUBuYXZlci5jb20iLCJyb2xlcyI6IlJPTEVfVVNFUiIsInVzZXJJZCI6OSwiaWF0IjoxNzIwMzU2MzcwLCJleHAiOjE3MjE1NjU5NzB9.4-_kGSG9IOHdsYSk79ihefHCZu8V-utJ1t-eYDo2570BJdlauttgaI-ig0RHCgYdNJOPT0KRvc-LpfAH_1hZSg',
        null, null);

insert into review (user_id, alcohol_id, content, size_type, price, zip_code, address, detail_address,
                    status, image_url, view_count, active_status, create_at, create_by, last_modify_at,
                    last_modify_by)
values (2, 1, '식별자 1번 임의리 리뷰', 'GLASS', 20000.00, '12345', '123 Main St', 'Apt 4B', 'PUBLIC',
        'https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1', null, null, '2024-06-08 16:29:01', null,
        '2024-06-19 20:44:58', null),
       (2, 1, '식별자 2번 임의리 리뷰입니다.', 'GLASS', 20000.00, '12345', '123 Main St', 'Apt 4B', 'PUBLIC',
        'https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1', null, null, '2024-06-08 16:29:01', null,
        '2024-06-19 20:44:58', null),
       (2, 1, '맛있게 잘 먹었습니다. ', 'GLASS', 20000.00, '12345', '123 Main St', 'Apt 4B', 'PRIVATE',
        'https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1', null, null, '2024-06-24 00:57:37', null,
        '2024-06-24 00:59:48', null),
       (2, 2, '맛있게 잘 먹었습니다.ggg ', 'GLASS', 20000.00, '12345', '123 Main St', 'Apt 4B', 'PUBLIC',
        'https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1', null, null, '2024-06-24 23:13:50', null,
        '2024-06-24 23:13:50', null);

insert into review_reply (review_id, user_id, root_reply_id, parent_reply_id, content, create_at, create_by,
                          last_modify_at, last_modify_by)
values (4, 3, null, null, '1 Root댓글', '2024-06-27 23:16:16', null, '2024-06-27 23:16:16', null),
       (4, 3, 1, 1, '👍👍👍👍', '2024-06-27 23:16:23', null, '2024-06-27 23:16:23', null),
       (4, 3, 1, 2, '👍👍👍👍', '2024-06-27 23:16:39', null, '2024-06-27 23:16:39', null),
       (4, 3, 1, 2, '👍👍👍👍', '2024-06-27 23:16:43', null, '2024-06-27 23:16:43', null),
       (4, 3, 1, 4, '👍👍👍👍', '2024-06-28 02:55:46', null, '2024-06-28 02:55:46', null),
       (4, 3, null, null, '2 Root 댓글', '2024-06-27 23:16:16', null, '2024-06-27 23:16:16', null),
       (4, 3, 1, 4, '🌿🌿🌿🌿🌿', '2024-06-28 02:55:46', null, '2024-06-28 02:55:46', null);
