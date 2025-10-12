-- Daily Data Report Table
CREATE TABLE IF NOT EXISTS daily_data_reports
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'PK',
    report_date        DATE         NOT NULL COMMENT '리포트 날짜',
    new_users_count    BIGINT       NOT NULL DEFAULT 0 COMMENT '신규 유저 수',
    new_reviews_count  BIGINT       NOT NULL DEFAULT 0 COMMENT '신규 리뷰 수',
    new_replies_count  BIGINT       NOT NULL DEFAULT 0 COMMENT '신규 댓글 수',
    new_likes_count    BIGINT       NOT NULL DEFAULT 0 COMMENT '신규 좋아요 수',
    webhook_sent       BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '웹훅 전송 여부',
    webhook_sent_at    DATETIME COMMENT '웹훅 전송 시간',
    create_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    last_modify_at     DATETIME COMMENT '수정일',
    UNIQUE KEY idx_report_date (report_date),
    INDEX idx_create_at (create_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '일일 데이터 리포트';
