package app.batch.bottlenote.job.popularity;

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
 * 관측 SQL을 실제로 실행해 보기 위한 인메모리 DB.
 *
 * <p>관측·적재 SQL은 문자열이라 컴파일러가 검증해 주지 않는다. 컬럼 오타 하나가 배포 후에야 드러나는 것을 막으려면 어딘가에서 한 번은 실행해 봐야 한다.
 *
 * <p>관측 테이블 DDL은 마이그레이션 파일을 그대로 읽어서 쓴다. 손으로 옮겨 적으면 그 사본이 낡아 검증 의미가 사라진다.
 */
final class ObservationSqlSupport {

  private static final Pattern MYSQL_TABLE_OPTIONS =
      Pattern.compile("\\)\\s*ENGINE=\\w+[^;]*?;", Pattern.DOTALL);

  private ObservationSqlSupport() {}

  static JdbcTemplate freshDatabase() {
    org.h2.Driver driver = new org.h2.Driver();
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriver(driver);
    // DB_CLOSE_DELAY=-1이 없으면 JdbcTemplate이 연결을 닫을 때마다 인메모리 DB가 통째로 사라진다
    dataSource.setUrl(
        "jdbc:h2:mem:observation-"
            + System.nanoTime()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");

    JdbcTemplate jdbc = new JdbcTemplate((DataSource) dataSource);
    createSourceTables(jdbc);
    createObservationTables(jdbc);
    return jdbc;
  }

  /** 관측이 읽는 원본 테이블. 실제 스키마에서 이 배치가 참조하는 컬럼만 추린다. */
  private static void createSourceTables(JdbcTemplate jdbc) {
    jdbc.execute(
        """
        CREATE TABLE alcohols (
          id BIGINT NOT NULL PRIMARY KEY,
          deleted_at TIMESTAMP NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE alcohols_view_histories (
          user_id BIGINT NOT NULL,
          alcohol_id BIGINT NOT NULL,
          view_at TIMESTAMP NOT NULL,
          PRIMARY KEY (user_id, alcohol_id)
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE ratings (
          alcohol_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          rating DOUBLE NOT NULL DEFAULT 0,
          PRIMARY KEY (alcohol_id, user_id)
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE picks (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          alcohol_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          status VARCHAR(255) NOT NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE reviews (
          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          alcohol_id BIGINT NOT NULL,
          user_id BIGINT NOT NULL,
          status VARCHAR(255) NULL,
          active_status VARCHAR(255) NULL
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
  }

  /** V14 마이그레이션을 그대로 읽어 적용한다. H2가 모르는 테이블 옵션만 벗겨낸다. */
  private static void createObservationTables(JdbcTemplate jdbc) {
    String ddl = readMigration();
    for (String statement : ddl.split(";")) {
      String sql = stripComments(statement).trim();
      if (sql.isEmpty()) {
        continue;
      }
      // 기존 테이블에 거는 인덱스는 위에서 만든 축약 원본에 그대로 적용된다
      jdbc.execute(sql);
    }
  }

  private static String readMigration() {
    Path path =
        Path.of("..", "git.environment-variables", "storage", "db", "migration")
            .resolve("V14__add_popularity_observation_tables.sql")
            .toAbsolutePath()
            .normalize();
    try {
      String raw = Files.readString(path, StandardCharsets.UTF_8);
      Matcher matcher = MYSQL_TABLE_OPTIONS.matcher(raw);
      return matcher.replaceAll(");");
    } catch (IOException e) {
      throw new IllegalStateException("V14 마이그레이션을 읽을 수 없습니다: " + path, e);
    }
  }

  private static String stripComments(String sql) {
    StringBuilder out = new StringBuilder();
    for (String line : sql.split("\n")) {
      if (line.trim().startsWith("--")) {
        continue;
      }
      out.append(line).append('\n');
    }
    return out.toString();
  }
}
