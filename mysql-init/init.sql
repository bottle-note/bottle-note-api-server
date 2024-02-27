CREATE TABLE IF NOT EXISTS users
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
;
