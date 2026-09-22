package app.batch.bottlenote.job.ranking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * 선정 SQL을 실제로 실행해 보기 위한 인메모리 DB.
 *
 * <p>원본 테이블은 이 배치가 읽는 컬럼만 추리고, 로그 테이블은 V20 마이그레이션을 그대로 읽어 만든다.
 */
final class BestReviewSqlSupport {

  private static final Pattern MYSQL_TABLE_OPTIONS =
      Pattern.compile("\\)\\s*ENGINE=\\w+[^;]*?;", Pattern.DOTALL);
  private static final Pattern MYSQL_CHARSET_CLAUSE =
      Pattern.compile("CHARACTER SET \\w+ COLLATE \\w+");

  private BestReviewSqlSupport() {}

  static JdbcTemplate freshDatabase() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriver(new org.h2.Driver());
    dataSource.setUrl(
        "jdbc:h2:mem:best-review-"
            + System.nanoTime()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");

    JdbcTemplate jdbc = new JdbcTemplate((DataSource) dataSource);
    createSourceTables(jdbc);
    createLogTable(jdbc);
    return jdbc;
  }

  private static void createSourceTables(JdbcTemplate jdbc) {
    jdbc.execute(
        """
        CREATE TABLE reviews (
          id BIGINT NOT NULL PRIMARY KEY,
          alcohol_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          content VARCHAR(1000) NOT NULL,
          status VARCHAR(255) NULL,
          active_status VARCHAR(255) NULL,
          is_best BOOLEAN NOT NULL DEFAULT FALSE
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE likes (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          review_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          status VARCHAR(255) NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE review_replies (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          review_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          status VARCHAR(255) NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE review_images (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          review_id BIGINT NOT NULL
        )
        """);
  }

  private static void createLogTable(JdbcTemplate jdbc) {
    for (String statement : readMigration().split(";")) {
      String sql = stripComments(statement).trim();
      if (!sql.isEmpty()) {
        jdbc.execute(sql);
      }
    }
  }

  private static String readMigration() {
    Path path =
        Path.of("..", "git.environment-variables", "storage", "db", "migration")
            .resolve("V20__add_best_review_selection_logs.sql")
            .toAbsolutePath()
            .normalize();
    try {
      String raw = Files.readString(path, StandardCharsets.UTF_8);
      Matcher matcher = MYSQL_TABLE_OPTIONS.matcher(raw);
      String withoutOptions = matcher.replaceAll(");");
      return MYSQL_CHARSET_CLAUSE.matcher(withoutOptions).replaceAll("");
    } catch (IOException e) {
      throw new IllegalStateException("V20 마이그레이션을 읽을 수 없습니다: " + path, e);
    }
  }

  private static String stripComments(String sql) {
    StringBuilder out = new StringBuilder();
    for (String line : sql.split("\n")) {
      if (!line.trim().startsWith("--")) {
        out.append(line).append('\n');
      }
    }
    return out.toString();
  }
}
