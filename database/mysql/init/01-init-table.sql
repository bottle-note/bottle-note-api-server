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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '국가';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '증류소';


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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '술';

CREATE TABLE `users`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '사용자',
    `email`          varchar(255) NOT NULL COMMENT '사용자 소셜 이메일',
    `nick_name`      varchar(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            Integer      NULL COMMENT '사용자 나이',
    `image_url`      varchar(255) NULL COMMENT '사용자 프로필 이미지',
    `gender`         varchar(255) NULL COMMENT '사용자 성별',
    `role`           varchar(255) NOT NULL COMMENT '사용자 역할' DEFAULT 'GUEST',
    `social_type`    varchar(255) NOT NULL COMMENT '소셜 타입 ( NAVER  ,GOOGLE , APPLIE )',
    `refresh_token`  varchar(255) NULL COMMENT 'access token 재발급을 위한 토큰',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`),
    UNIQUE KEY `email` (`email`),
    UNIQUE KEY `nick_name` (`nick_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '사용자';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '찜하기';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '유저 신고';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '술 평점';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '문의';



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
--   복합 유니크 UNIQUE KEY `user_id_follow_user_id` (`user_id`, `follow_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '팔로우';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '테이스팅 태그';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '술/테이스팅 태그 연관관계 해소';

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
    `create_at`      timestamp      NULL COMMENT '최초 생성일',
    `create_by`      varchar(255)   NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp      NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255)   NULL COMMENT '최종 생성자',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    FOREIGN KEY (`alcohol_id`) REFERENCES `alcohol` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '술 리뷰';


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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '리뷰 신고';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '리뷰-이미지 등록은 최대 5장';

CREATE TABLE `review_tasting_tag`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '리뷰 테이스팅 태그 - 최대 10개',
    `review_id`      bigint      NOT NULL comment '리뷰 아이디',
    `tasting_tag`    varchar(12) NOT NULL COMMENT '테이스팅 태그 - 최대 12자',
    `create_at`      timestamp   NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp   NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '리뷰 테이스팅 태그';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '리뷰 댓글';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '공지사항';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '좋아요';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '술 이미지';

CREATE TABLE `user_history`
(
    `id`             bigint       NOT NULL COMMENT '히스토리 id',
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
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT = '유저 히스토리';
