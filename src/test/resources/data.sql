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

CREATE TABLE `users`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '사용자',
    `email`          varchar(255) NOT NULL COMMENT '사용자 소셜 이메일',
    `nick_name`      varchar(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            Integer      NULL COMMENT '사용자 나이',
    `image_url`      varchar(255) NULL COMMENT '사용자 프로필 이미지',
    `gender`         varchar(255) NULL COMMENT '사용자 성별',
    `role`           varchar(255) NOT NULL DEFAULT 'GUEST' COMMENT '사용자 역할',
    `social_type`    varchar(255) NOT NULL COMMENT '소셜 타입 ( NAVER  ,GOOGLE , APPLIE )',
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
    -- 복합 유니크 UNIQUE KEY `user_id_report_user` (`user_id`, `report_user`)
);

CREATE TABLE `rating`
(
    `alcohol_id`     bigint           NOT NULL COMMENT '평가 대상 술',
    `user_id`        bigint           NOT NULL COMMENT '평가자(사용자)',
    `rating`         DOUBLE PRECISION NOT NULL DEFAULT 0 COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      timestamp        NULL COMMENT '최초 생성일',
    `create_by`      varchar(255)     NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp        NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255)     NULL COMMENT '최종 생성자',
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
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`follow_user_id`) REFERENCES `users` (`id`)
--   복합 유니크 UNIQUE KEY `user_id_follow_user_id` (`user_id`, `follow_user_id`)
);

CREATE TABLE `tasting_tag`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '테이스팅 태그',
    `kor_name`       varchar(255) NOT NULL COMMENT '한글 태그 이름',
    `eng_name`       varchar(255) NOT NULL COMMENT '영문 태그 이름',
    `icon`           varchar(255) NULL COMMENT '앱 출시 후 디벨롭 할 때 사용',
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
    `content`        varchar(255)   NOT NULL COMMENT '1000글자',
    `size_type`      varchar(255)   NULL COMMENT '잔 : GLASS , 보틀 : BOTTLE',
    `price`          DECIMAL(10, 2) NULL COMMENT '가격',
    `zip_code`       varchar(255)   NULL COMMENT '마신 장소 우편번호',
    `address`        varchar(255)   NULL COMMENT '마신 장소 주소',
    `detail_address` varchar(255)   NULL COMMENT '마신 장소 상세 주소',
    `status`         varchar(255)   NULL COMMENT '공개리뷰, 숨김리뷰',
    `image_url`      varchar(255)   NULL COMMENT '썸네일 이미지',
    `view_count`     bigint         NULL COMMENT '조회수',
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
    `image_url`      varchar(255) NOT NULL COMMENT 'S3 이미지 경로',
    `file_name`      varchar(255) NOT NULL COMMENT '규칙에 맞게 수정된 이미지 파일 명',
    `file_org_name`  varchar(255) NOT NULL COMMENT '업로드된 파일 명',
    `file_size`      varchar(255) NOT NULL COMMENT '업로드된 파일 크기 (최대 사이즈 지정 필요)',
    `order`          bigint       NOT NULL COMMENT '이미지 순서',
    `status`         varchar(255) NULL COMMENT '삭제됨 / 숨김처리됨 / 유효기간이 만료됨 등등',
    `tags`           varchar(255) NULL COMMENT '이미지의 태그 S3에서 사용될 값',
    `description`    varchar(255) NULL COMMENT '이미지 주석 S3에서 사용될 값',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
);

CREATE TABLE `review_reply`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰 댓글',
    `review_id`       bigint       NOT NULL COMMENT '리뷰 아이디',
    `user_id`         bigint       NOT NULL COMMENT '리뷰 작성자',
    `parent_reply_id` bigint       NULL comment '부모 댓글 아이디',
    `content`         text         NOT NULL COMMENT '댓글 최대 1000글자',
    `create_at`       timestamp    NULL COMMENT '최초 생성일',
    `create_by`       varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at`  timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by`  varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`parent_reply_id`) REFERENCES `review_reply` (`id`)
    -- 복합 유니크 UNIQUE KEY `review_id_user_id` (`review_id`, `user_id`)
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

CREATE TABLE `like`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '좋아요',
    `review_id`      bigint       NOT NULL COMMENT '좋아요의 대상 리뷰',
    `users_id`       bigint       NOT NULL COMMENT '좋아요를 누른 사람',
    `status`         varchar(255) NULL COMMENT '공감, 공감취소',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`),
    FOREIGN KEY (`users_id`) REFERENCES `users` (`id`)
);

CREATE TABLE `alcohol_image`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '술 이미지',
    `alcohol_id`     bigint       NOT NULL COMMENT '술 아이디',
    `image_url`      varchar(255) NOT NULL COMMENT 'S3 이미지 경로',
    `file_name`      varchar(255) NOT NULL COMMENT '규칙에 맞게 수정된 이미지 파일 명',
    `file_size`      varchar(255) NULL COMMENT '업로드된 파일 크기 (최대 사이즈 지정 필요)',
    `order`          varchar(255) NOT NULL COMMENT '이미지 순서',
    `status`         varchar(255) NULL COMMENT '삭제됨 / 숨김처리됨 / 유효기간이 만료됨 등등',
    `tags`           varchar(255) NULL COMMENT '이미지의 태그 S3에서 사용될 값',
    `description`    varchar(255) NULL COMMENT '이미지 주석',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
);


INSERT INTO region (kor_name, eng_name, continent, description)
VALUES ('스코틀랜드/로우랜드', 'Scotland/Lowlands', '유럽', '가벼운 맛이 특징인 로우랜드 위스키'),
       ('스코틀랜드/하이랜드', 'Scotland/Highlands', '유럽', '맛의 다양성이 특징인 하이랜드 위스키, 해안의 짠맛부터 달콤하고 과일 맛까지'),
       ('스코틀랜드/아일레이', 'Scotland/Islay', '유럽', '강렬한 피트 맛과 연기 맛이 특징인 아일레이 위스키'),
       ('스코틀랜드', 'Scotland/Islands', '유럽', '풍부하고 복잡한 맛'),
       ('스코틀랜드/스페이사이드', 'Scotland/Speyside', '유럽', '과일 맛이 강하고 부드러운 스페이사이드 위스키'),
       ('스코틀랜드/캠벨타운', 'Scotland/Campbeltown', '유럽', '풍미가 강하고 소금기가 있는 캠벨타운 위스키'),
       ('인도', 'India', '아시아', '다양한 스타일의 위스키를 생산하는 인도'),
       ('일본', 'Japan', '아시아', '섬세하고 균형 잡힌 맛의 일본 위스키'),
       ('미국', 'United States', '북아메리카', '버번과 테네시 위스키를 포함한 다양한 스타일'),
       ('아일랜드', 'Ireland', '유럽', '부드럽고 향긋한 아일랜드 위스키'),
       ('프랑스', 'France', '유럽', '주로 브랜디와 와인 생산지로 유명하지만 위스키도 생산'),
       ('스웨덴', 'Sweden', '유럽', '실험적인 방법으로 만드는 스웨덴 위스키'),
       ('덴마크', 'Denmark', '유럽', '전통적이고 현대적인 방식을 결합한 덴마크 위스키'),
       ('영국', 'United Kingdom', '유럽', '전통적인 영국 스타일의 위스키'),
       ('캐나다', 'Canada', '북아메리카', '라이 위스키가 유명한 캐나다'),
       ('네덜란드', 'Netherlands', '유럽', '유럽 스타일의 위스키를 제조하는 네덜란드'),
       ('독일', 'Germany', '유럽', '다양한 지역 위스키가 생산되는 독일'),
       ('체코', 'Czech Republic', '유럽', '새로운 위스키 생산지로 떠오르는 체코'),
       ('오스트레일리아', 'Australia', '오세아니아', '현대적인 방식으로 위스키를 생산하는 오스트레일리아'),
       ('이스라엘', 'Israel', '아시아', '독특한 기후에서 생산되는 이스라엘의 위스키'),
       ('스위스', 'Switzerland', '유럽', '소규모로 생산되는 스위스 위스키'),
       ('웨일즈', 'Wales', '유럽', '전통적인 방법으로 생산되는 웨일즈의 위스키'),
       ('타이완', 'Taiwan', '아시아', '고품질의 위스키를 생산하는 타이완'),
       ('필란드', 'Finland', '유럽', '추운 기후에서 생산되는 필란드 위스키');


INSERT INTO distillery(kor_name, eng_name)
VALUES ('글래스고 디스틸러리', 'The Glasgow Distillery Co.'),
       ('노크두', 'Knockdhu'),
       ('아드벡', 'Ardbeg'),
       ('아란', 'Arran'),
       ('에버라워', 'Aberlour'),
       ('블렌드', 'blend'),
       ('아므르트', 'Amrut'),
       ('에버펠디', 'Aberfeldy'),
       ('아사카', 'Asaka'),
       ('야마자키', 'Yamazaki'),
       ('오첸토샨', 'Auchentoshan'),
       ('와일드 터키 디스틸러리', 'Wild Turkey Distillery'),
       ('요이치', 'Yoichi'),
       ('우드퍼드 리저브', 'Woodford Reserve'),
       ('울프번', 'Wolfburn'),
       ('웨스트랜드 디스틸러리', 'Westland Distillery'),
       ('위도 제인 디스틸러리', 'Widow Jane Distillery'),
       ('윌렛 디스틸러리', 'Willett Distillery'),
       ('툴리바딘', 'Tullibardine'),
       ('쿨리', 'Cooley'),
       ('페터케언', 'Fettercairn'),
       ('에이가시마 슈조', 'Eigashima Shuzo'),
       ('글렌알라키', 'Glenallachie'),
       ('위슬피그', 'WhistlePig'),
       ('아드모어', 'Ardmore'),
       ('아드나머칸', 'Ardnamurchan'),
       ('바렌겜', 'Warenghem'),
       ('올트모어', 'Aultmore'),
       ('발블레어', 'Balblair'),
       ('발코니스 디스틸링', 'Balcones Distilling'),
       ('에드라도어', 'Edradour'),
       ('발베니', 'Balvenie'),
       ('바렐 크래프트 스피릿', 'Barrell Craft Spirits'),
       ('벤 네비스', 'Ben Nevis'),
       ('벤리아크', 'BenRiach'),
       ('벤린네스', 'Benrinnes'),
       ('벤로마크', 'Benromach'),
       ('베르그슬라겐스 데스틸레리', 'Bergslagens Destilleri'),
       ('블라드녹', 'Bladnoch'),
       ('블레어 애솔', 'Blair Athol'),
       ('버팔로 트레이스 디스틸러리', 'Buffalo Trace Distillery'),
       ('보우모어', 'Bowmore'),
       ('브라운스테인', 'Braunstein'),
       ('브루이클라디', 'Bruichladdich'),
       ('버나베인', 'Bunnahabhain'),
       ('부시밀스', 'Bushmills'),
       ('카올 일라', 'Caol Ila'),
       ('카퍼도니치', 'Caperdonich'),
       ('카드후', 'Cardhu'),
       ('치치부', 'Chichibu'),
       ('클레이 디스틸러리', 'Cley Distillery'),
       ('클로나킬티 디스틸러리', 'Clonakilty Distillery'),
       ('클리넬리시', 'Clynelish'),
       ('크래간모어', 'Cragganmore'),
       ('크레이겔라키', 'Craigellachie'),
       ('데일루에인', 'Dailuaine'),
       ('달모어', 'Dalmore'),
       ('달위니', 'Dalwhinnie'),
       ('딘스턴', 'Deanston'),
       ('딩글 위스키 디스틸러리', 'The Dingle Whiskey Distillery');


INSERT INTO alcohol (id, kor_name, eng_name, abv, type, kor_category, eng_category, region_id,
                     distillery_id, age, cask, image_url)
VALUES (1, '글래스고 1770 싱글몰트 스카치 위스키', '1770 Glasgow Single Malt', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 2, 1, null,
        'Marriage of Ex-Bourbon & Virgin Oak Casks',
        'https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg'),
       (2, '글래스고 1770 싱글몰트 스카치 위스키', '1770 Glasgow Single Malt', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 2, 1, null,
        'Virgin Oak & PX Sherry Cask Finish', 'https://static.whiskybase.com/storage/whiskies/2/0/8888/404535-big.jpg'),
       (3, '글래스고 1770 싱글몰트 스카치 위스키', '1770 Glasgow Single Malt', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 2, 1, null,
        '1st fill ex-bourbon, finish virgin oak',
        'https://static.whiskybase.com/storage/whiskies/2/1/1644/404542-big.jpg'),
       (4, '아녹 18년', 'anCnoc 18-year-old', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 2, '18', 'Bourbon & Sherry',
        'https://static.whiskybase.com/storage/whiskies/6/3/017/397016-big.jpg'),
       (5, '아녹 24년', 'anCnoc 24-year-old', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 2, '24', null,
        'https://static.whiskybase.com/storage/whiskies/6/6/989/270671-big.jpg'),
       (6, '아녹 쉐리캐스크 피니시', 'anCnoc Sherry Cask Finish', '43', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 2, null,
        'Sherry Finish', 'https://static.whiskybase.com/storage/whiskies/2/3/4986/429641-big.jpg'),
       (7, '아녹 피티드하트', 'anCnoc peatheart', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 2, null, 'Bourbon Barrels',
        'https://static.whiskybase.com/storage/whiskies/1/0/3078/167533-big.jpg'),
       (8, '아드백 10년', 'Ardbeg Ten', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, '10', 'Bourbon Barrel',
        'https://static.whiskybase.com/storage/whiskies/2/4/6529/442975-big.jpg'),
       (9, '아드백 10년', 'Ardbeg Ten', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, '10', 'Bourbon Barrel',
        'https://static.whiskybase.com/storage/whiskies/2/5/635/444120-big.jpg'),
       (10, '아드백 우거다일', 'Ardbeg Uigeadail', '54.2', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, null,
        'Bourbon / Sherry Casks', 'https://static.whiskybase.com/storage/whiskies/1/3/4603/400899-big.jpg'),
       (11, '아드백 위비스티', 'Ardbeg Wee Beastie', '47.4', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, '5',
        'Ex-Bourbon and Oloroso Sherry Casks',
        'https://static.whiskybase.com/storage/whiskies/1/5/2827/366361-big.jpg'),
       (12, '아드백 코리브레칸', 'Ardbeg Corryvreckan', '57.1', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, null,
        'Bourbon & New French Oak', 'https://static.whiskybase.com/storage/whiskies/1/3/3030/401385-big.jpg'),
       (13, '아드벡 언 오', 'Ardbeg An Oa', '46.6', 'WHISKY', '싱글 몰트', 'Single Malt', 4, 3, null,
        'Charred New Oak, PX Sherry & 1st Fill Bourbon',
        'https://static.whiskybase.com/storage/whiskies/9/9/885/161333-big.jpg'),
       (14, '아란 12년', 'Arran 10-year-old', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, '10', null,
        'https://static.whiskybase.com/storage/whiskies/2/3/2004/399512-big.jpg'),
       (15, '아란 18년', 'Arran 18-year-old', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, '18', 'Sherry Casks',
        'https://static.whiskybase.com/storage/whiskies/1/4/3180/241151-big.jpg'),
       (16, '아란 21년', 'Arran 21-year-old', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, '21', null,
        'https://static.whiskybase.com/storage/whiskies/1/4/2044/240931-big.jpg'),
       (17, '아란 소테른 캐스크 피니시', 'Arran Sauternes Cask Finish', '50', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, null,
        'Sauternes Cask Finish', 'https://static.whiskybase.com/storage/whiskies/2/0/9532/368792-big.jpg'),
       (18, '아란 쉐리 캐스크', 'Arran Sherry Cask', '55.8', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, null, 'Sherry Hogshead',
        'https://static.whiskybase.com/storage/whiskies/2/0/9608/368892-big.jpg'),
       (19, '아란 아마로네 캐스크 피니시', 'Arran Amarone Cask Finish', '50', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, null,
        'Amarone Cask Finish', 'https://static.whiskybase.com/storage/whiskies/2/1/2436/372421-big.jpg'),
       (20, '아란 쿼터 캐스크 피니시', 'Arran Quarter Cask', '56.2', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, null, 'Quarter Cask',
        'https://static.whiskybase.com/storage/whiskies/1/4/0149/234075-big.jpg'),
       (21, '아란 포트 캐스크 피니시', 'Arran Port Cask Finish', '50', 'WHISKY', '싱글 몰트', 'Single Malt', 5, 4, null,
        'Port Cask Finish', 'https://static.whiskybase.com/storage/whiskies/2/0/9534/414273-big.jpg'),
       (22, '아벨라워 12년', 'Aberlour 12-year-old', '40', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '12',
        'American Oak Casks, Sherry Oak Casks',
        'https://static.whiskybase.com/storage/whiskies/1/6/1049/442050-big.jpg'),
       (23, '아벨라워 12년', 'Aberlour 12-year-old', '48', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '12',
        'American Oak & Sherry Oak Casks', 'https://static.whiskybase.com/storage/whiskies/2/1/3952/374849-big.jpg'),
       (24, '아벨라워 12년', 'Aberlour 10-year-old', '40', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '10',
        'Bourbon & Sherry, French Limousin Oak Finish',
        'https://static.whiskybase.com/storage/whiskies/1/3/4377/266645-big.jpg'),
       (25, '아벨라워 14년', 'Aberlour 14-year-old', '40', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '14',
        'American Oak, Oloroso Sherry', 'https://static.whiskybase.com/storage/whiskies/2/3/7128/424097-big.jpg'),
       (26, '아벨라워 16년', 'Aberlour 16-year-old', '43', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '16',
        'Oak & Sherry Casks', 'https://static.whiskybase.com/storage/whiskies/2/3/7130/441412-big.jpg'),
       (27, '아벨라워 18년', 'Aberlour 18-year-old', '43', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '18', 'Sherry Casks',
        'https://static.whiskybase.com/storage/whiskies/1/1/7552/208121-big.jpg'),
       (28, '아벨라워 18년', 'Aberlour 18-year-old', '43', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, '18',
        'Am. & Eur. Oak + 1st-Fill PX & Oloroso Finish',
        'https://static.whiskybase.com/storage/whiskies/2/3/7131/424101-big.jpg'),
       (29, '아벨라워 캐스트 안남', 'Aberlour Casg Annamh', '48', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, null,
        'American and Sherry Oak Cask', 'https://static.whiskybase.com/storage/whiskies/2/0/2405/363739-big.jpg'),
       (30, '아벨라워 캐스트 안남', 'Aberlour Casg Annamh', '48', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, null,
        '2 types American oak, Oloroso sherry',
        'https://static.whiskybase.com/storage/whiskies/2/1/6802/396563-big.jpg'),
       (31, '아벨라워 캐스트 안남', 'Aberlour Casg Annamh', '48', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, null,
        'American Oak and Sherry', 'https://static.whiskybase.com/storage/whiskies/2/3/5293/434914-big.jpg'),
       (32, '아벨라워 캐스트 안남', 'Aberlour Casg Annamh', '48', 'WHISKY', '싱글 몰트', 'Single Malt', 6, 5, null,
        'American & Sherry Oak Casks', 'https://static.whiskybase.com/storage/whiskies/1/8/3005/324073-big.jpg'),
       (33, '안녹 12년', 'anCnoc 12-year-old', '40', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 6, '12', null,
        'https://static.whiskybase.com/storage/whiskies/2/3/9/54814-big.jpg'),
       (34, '암룻 인디안 싱글몰트 위스키', 'Amrut Indian Single Malt Whisky', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 8, 7, null,
        'Oak Barrels', 'https://static.whiskybase.com/storage/whiskies/1/3/8244/233284-big.jpg'),
       (35, '암룻 캐스크 스트렝스', 'Amrut Cask Strength', '61.8', 'WHISKY', '싱글 몰트', 'Single Malt', 8, 7, null, 'Barrel',
        'https://static.whiskybase.com/storage/whiskies/1/4/5274/258772-big.jpg'),
       (36, '암룻 퓨전', 'Amrut Fusion', '50', 'WHISKY', '싱글 몰트', 'Single Malt', null, 7, null, null,
        'https://static.whiskybase.com/storage/whiskies/2/4/1238/432384-big.jpg'),
       (37, '암룻 피티드 인디안', 'Amrut Peated Indian', '62.8', 'WHISKY', '싱글 몰트', 'Single Malt', 8, 7, null, 'Oak Barrels',
        'https://static.whiskybase.com/storage/whiskies/1/4/5272/271693-big.jpg'),
       (38, '암룻 피티드 인디안', 'Amrut Peated Indian', '46', 'WHISKY', '싱글 몰트', 'Single Malt', 8, 7, null, 'Oak Barrels',
        'https://static.whiskybase.com/storage/whiskies/1/5/0534/254736-big.jpg'),
       (39, '애버펠디 12년', 'Aberfeldy 12-year-old', '40', 'WHISKY', '싱글 몰트', 'Single Malt', 3, 8, '12', null,
        'https://static.whiskybase.com/storage/whiskies/7/6/497/120954-big.jpg');
