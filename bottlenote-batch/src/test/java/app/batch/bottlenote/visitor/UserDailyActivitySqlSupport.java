package app.batch.bottlenote.visitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * 롤업 SQL을 실제로 실행해 보기 위한 인메모리 DB.
 *
 * <p>대상 테이블 DDL은 V23 마이그레이션을 그대로 읽어서 쓴다. 손으로 옮겨 적으면 그 사본이 낡아 검증 의미가 사라진다.
 */
final class UserDailyActivitySqlSupport {

  private static final Pattern MYSQL_TABLE_OPTIONS =
      Pattern.compile("\\)\\s*ENGINE=\\w+[^;]*?;", Pattern.DOTALL);

  private UserDailyActivitySqlSupport() {}

  static JdbcTemplate freshDatabase() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriver(new org.h2.Driver());
    // DB_CLOSE_DELAY=-1이 없으면 JdbcTemplate이 연결을 닫을 때마다 인메모리 DB가 통째로 사라진다
    dataSource.setUrl(
        "jdbc:h2:mem:user-daily-activity-"
            + System.nanoTime()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");

    JdbcTemplate jdbc = new JdbcTemplate((DataSource) dataSource);
    // 롤업이 읽는 원본 컬럼만 추린다
    jdbc.execute(
        """
        CREATE TABLE visitor_telemetry_events (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          occurred_at TIMESTAMP(6) NOT NULL,
          visitor_id VARCHAR(64) NOT NULL,
          user_id BIGINT NULL
        )
        """);
    for (String statement : readMigration().split(";")) {
      String sql = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
      if (!sql.isEmpty()) {
        jdbc.execute(sql);
      }
    }
    return jdbc;
  }

  private static String readMigration() {
    Path path =
        Path.of("..", "git.environment-variables", "storage", "db", "migration")
            .resolve("V23__add_user_daily_activities.sql")
            .toAbsolutePath()
            .normalize();
    try {
      String raw = Files.readString(path, StandardCharsets.UTF_8);
      return MYSQL_TABLE_OPTIONS.matcher(raw).replaceAll(");");
    } catch (IOException e) {
      throw new IllegalStateException("V23 마이그레이션을 읽을 수 없습니다: " + path, e);
    }
  }
}
