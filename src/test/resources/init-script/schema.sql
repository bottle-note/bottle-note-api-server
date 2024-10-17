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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '국가';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '증류소';

CREATE TABLE `alcohol`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '술',
    `kor_name`       varchar(255) NOT NULL COMMENT '한글 이름',
    `eng_name`       varchar(255) NOT NULL COMMENT '영문 이름',
    `abv`            varchar(255) NULL COMMENT '도수',
    `type`           varchar(255) NOT NULL COMMENT '위스키 고정 ( 추후 럼,진등으로 확장 가능)',
    `kor_category`   varchar(255) NOT NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리 한글명',
    `eng_category`   varchar(255) NOT NULL COMMENT '위스키, 럼, 브랜디의 하위상세 카테고리 영문명 ',
    `category_group` varchar(255) NOT NULL COMMENT '하위 카테고리 그룹',
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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '술';

CREATE TABLE `users`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '사용자',
    `email`          varchar(255) NOT NULL COMMENT '사용자 소셜 이메일',
    `nick_name`      varchar(255) NOT NULL COMMENT '사용자 소셜 닉네임 ( 수정 가능 )',
    `age`            Integer      NULL COMMENT '사용자 나이',
    `image_url`      varchar(255) NULL COMMENT '사용자 프로필 이미지',
    `gender`         varchar(255) NULL COMMENT '사용자 성별',
    `role`           varchar(255) NOT NULL COMMENT '사용자 역할' DEFAULT 'GUEST',
    `status`      varchar(255) NOT NULL COMMENT '사용자 상태',
    `social_type` json NOT NULL COMMENT '소셜 타입 ( NAVER  ,GOOGLE , APPLE )',
    `refresh_token`  varchar(255) NULL COMMENT 'access token 재발급을 위한 토큰',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    PRIMARY KEY (`id`),
    UNIQUE KEY `email` (`email`),
    UNIQUE KEY `nick_name` (`nick_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '사용자';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '찜하기';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '유저 신고';

CREATE TABLE `rating`
(
    `alcohol_id`     bigint       NOT NULL COMMENT '평가 대상 술',
    `user_id`        bigint       NOT NULL COMMENT '평가자(사용자)',
    `rating`         DOUBLE       NOT NULL DEFAULT 0 COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
    `create_at`      timestamp    NULL COMMENT '최초 생성일',
    `create_by`      varchar(255) NULL COMMENT '최초 생성자',
    `last_modify_at` timestamp    NULL COMMENT '최종 생성일',
    `last_modify_by` varchar(255) NULL COMMENT '최종 생성자',
    PRIMARY KEY (`alcohol_id`, `user_id`),
    foreign key (`alcohol_id`) references `alcohol` (`id`),
    foreign key (`user_id`) references `users` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '술 평점';

CREATE TABLE `help`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '문의',
    `user_id`          bigint       NOT NULL COMMENT '문의자',
    `type`             varchar(255) NOT NULL COMMENT 'ADD , USER... 개발때 enum 추가',
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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '문의';

CREATE TABLE `help_image`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰-이미지 등록은 최대 5장',
    `help_id`        bigint       NOT NULL comment '문의글 아이디',
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
    FOREIGN KEY (`help_id`) REFERENCES `help` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '문의-이미지 등록은 최대 5장';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '팔로우';

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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '테이스팅 태그';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '술/테이스팅 태그 연관관계 해소';

CREATE TABLE `review`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT COMMENT '술 리뷰',
    `user_id`        bigint         NOT NULL COMMENT '리뷰 작성자',
    `alcohol_id`     bigint         NOT NULL COMMENT '리뷰 대상 술',
    `content`        varchar(1000)  NOT NULL COMMENT '1000글자',
    `size_type`      varchar(255)   NULL COMMENT '잔 : GLASS , 보틀 : BOTTLE',
    `price`          decimal(38, 2) NULL COMMENT '가격',
    `location_name`       varchar(255)   NULL COMMENT '상호 명',
    `street_address` varchar(255)   NULL COMMENT '도로명 주소',
    `category`       varchar(255)   NULL COMMENT '장소 카테고리',
    `map_url`        varchar(255)   NULL COMMENT '지도 URL',
    `latitude`       varchar(255)   NULL COMMENT '위도 (x좌표)',
    `longitude`      varchar(255)   NULL COMMENT '경도 (y좌표)',
    `status`         varchar(255)   NULL COMMENT '리뷰 상태',
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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '리뷰 신고';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '리뷰-이미지 등록은 최대 5장';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '리뷰 테이스팅 태그';

CREATE TABLE `review_reply`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '리뷰 댓글',
    `review_id`       bigint       NOT NULL COMMENT '리뷰 아이디',
    `user_id`         bigint       NOT NULL COMMENT '리뷰 작성자',
    `status`          varchar(255) NULL COMMENT '리뷰 댓글의 현재 상태',
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
    foreign key (`root_reply_id`) references `review_reply` (`id`),
    FOREIGN KEY (`parent_reply_id`) REFERENCES `review_reply` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '리뷰 댓글';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '공지사항';

CREATE TABLE `likes`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '좋아요',
    `review_id`      bigint       NOT NULL COMMENT '좋아요의 대상 리뷰',
    `user_id`        bigint       NOT NULL COMMENT '좋아요를 누른 사용자 식별자',
    `user_nick_name` varchar(255) NOT NULL COMMENT '좋아요를 누른 사용자 닉네임',
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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '좋아요';

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
  COLLATE = utf8mb4_unicode_ci
    COMMENT
        = '술 이미지';

create table user_history
(
    id              bigint       not null comment '히스토리 id'
        primary key,
    user_id         bigint       not null comment '사용자 id',
    event_category  varchar(255) not null comment 'pick, review, rating',
    event_type      varchar(255) null comment 'isPick,unPick || like, create, review, best || start, modify, delete',
    redirect_url    varchar(255) null comment '발생되는 api의 도메인주소를 뺀 url',
    image_url       varchar(255) null comment '발생되는 api의 도메인주소를 뺀 url',
    alcohol_name    varchar(255) null comment '알코올 이름(한글)',
    message         varchar(255) null comment '이벤트 메세지 enum으로 관리',
    dynamic_message json         null comment '가변데이터(현재는 별점에서만 사용)',
    event_year      varchar(255) null comment '발생 년(YYYY)',
    event_month     varchar(255) null comment '발생 월(MM)',
    description     varchar(255) null comment '비고',
    create_at       timestamp    null,
    create_by       varchar(255) null,
    last_modify_at  timestamp    null comment '최종 생성일',
    last_modify_by  varchar(255) null comment '최종 생성자',
    constraint user_history_ibfk_1
        foreign key (user_id) references users (id)
)
    comment '유저 히스토리';

create table notification
(
    id             bigint auto_increment primary key,
    user_id        bigint       not null comment '사용자 id',
    title          varchar(255) not null comment '알림 제목',
    content        text         not null comment '알림 내용',
    type           varchar(255) not null comment '알림 타입 (SYSTEM: 시스템 알림, USER: 사용자 알림, PROMOTION: 프로모션 알림)',
    category       varchar(255) not null comment '알림의 종류 ( 리뷰, 댓글, 팔로우, 좋아요, 프로모션 )',
    status         varchar(255) not null comment '알림 상태 (PENDING: 대기 중, SENT: 전송됨, READ: 읽음, FAILED: 실패)',
    is_read        boolean      not null comment '읽음 여부',
    create_at      timestamp    null comment '최초 생성일',
    create_by      varchar(255) null comment '최초 생성자',
    last_modify_at timestamp    null comment '최종 수정일',
    last_modify_by varchar(255) null comment '최종 수정자',
    constraint notification_users_id_fk
        foreign key (user_id) references users (id)
)
    engine = InnoDB
    default charset = utf8mb4
    collate utf8mb4_unicode_ci
    comment
        = '사용자 알림';

create table popular_alcohol
(
    id            bigint auto_increment comment '기본 키'
        primary key,
    alcohol_id    bigint                              not null comment '술 ID',
    year          smallint                            not null comment '년도',
    month         tinyint                             not null comment '월',
    day           tinyint                             not null comment '일',
    review_score  decimal(5, 2)                       not null comment '리뷰 점수',
    rating_score  decimal(5, 2)                       not null comment '평점 점수',
    pick_score    decimal(5, 2)                       not null comment '찜하기 점수',
    popular_score decimal(5, 2)                       not null comment '인기도 점수',
    created_at    timestamp default CURRENT_TIMESTAMP null comment '생성일시',
    constraint uniq_alcohol_year_month
        unique (alcohol_id, year, month, day)
)
    comment '술 인기도 통계 테이블' charset = utf8mb4;

create table popularity_table
(
    alcohol_id       int   not null
        primary key,
    review_score     float null,
    rating_score     float null,
    pick_score       float null,
    popularity_score float null
);


