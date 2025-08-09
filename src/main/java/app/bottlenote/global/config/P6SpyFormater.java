package app.bottlenote.global.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyFormater implements MessageFormattingStrategy {

  @PostConstruct
  public void setLogMessageFormat() {
    P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());

    System.setProperty("p6spy.config.filter", "true");
    System.setProperty("p6spy.config.exclude", "QRTZ_");
  }

  @Override
  public String formatMessage(
      int connectionId,
      String now,
      long elapsed,
      String category,
      String prepared,
      String sql,
      String url) {
    String threadName = Thread.currentThread().getName();

    if (threadName.contains("Scheduler") || threadName.contains("eduler_Worker")) return "";
    if (sql != null && sql.toUpperCase().contains("QRTZ_")) return "";
    sql = formatSql(category, sql);
    return String.format("[%s] | %d ms | %s", category, elapsed, sql);
  }

  private String formatSql(String category, String sql) {
    if (sql != null && sql.toUpperCase().contains("QRTZ_")) {
      return ""; // 빈 문자열 반환하여 로깅 건너뛰기
    }

    if (sql != null && !sql.trim().isEmpty() && Category.STATEMENT.getName().equals(category)) {
      String trimmedSQL = sql.trim().toLowerCase(Locale.ROOT);
      if (trimmedSQL.startsWith("create")
          || trimmedSQL.startsWith("alter")
          || trimmedSQL.startsWith("comment")) {
        sql = FormatStyle.DDL.getFormatter().format(sql);
      } else {
        sql = FormatStyle.BASIC.getFormatter().format(sql);
      }
      return sql;
    }
    return sql;
  }
}
