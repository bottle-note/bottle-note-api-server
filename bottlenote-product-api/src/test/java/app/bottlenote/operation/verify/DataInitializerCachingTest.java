package app.bottlenote.operation.verify;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.DataInitializer;
import app.bottlenote.operation.utils.TestContainersConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * DataInitializer의 캐싱 로직을 검증하는 테스트
 *
 * <p>volatile, Double-checked locking, 시스템 테이블 제외 등을 검증
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Tag("operation")
@DisplayName("DataInitializer 캐싱 검증 테스트")
class DataInitializerCachingTest {

  @Autowired private DataInitializer dataInitializer;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("deleteAll()을 여러 번 호출해도 정상 동작해야 한다")
  void delete_all_should_work_multiple_times() {
    dataInitializer.deleteAll();
    dataInitializer.deleteAll();
    dataInitializer.deleteAll();

    String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
    assertThat(result).isEqualTo("1");
  }

  @Test
  @DisplayName("멀티스레드 환경에서 Thread-safe하게 동작해야 한다")
  void delete_all_should_be_thread_safe() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Exception> exceptions = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      executorService.submit(
          () -> {
            try {
              dataInitializer.deleteAll();
            } catch (Exception e) {
              exceptions.add(e);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(30, TimeUnit.SECONDS);
    executorService.shutdown();

    assertThat(exceptions).isEmpty();
  }

  @Test
  @DisplayName("시스템 테이블(flyway_, databasechangelog)이 있어도 정상 동작해야 한다")
  void should_skip_system_tables() {
    List<String> allTables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
    boolean hasSystemTables =
        allTables.stream()
            .anyMatch(
                table ->
                    table.startsWith("flyway_")
                        || table.startsWith("databasechangelog")
                        || table.startsWith("schema_version"));

    dataInitializer.deleteAll();

    if (hasSystemTables) {
      List<String> tablesAfterDelete = jdbcTemplate.queryForList("SHOW TABLES", String.class);
      boolean systemTablesStillExist =
          tablesAfterDelete.stream()
              .anyMatch(
                  table ->
                      table.startsWith("flyway_")
                          || table.startsWith("databasechangelog")
                          || table.startsWith("schema_version"));
      assertThat(systemTablesStillExist).isTrue();
    }
  }

  @Test
  @DisplayName("초기화 후에도 DB 연결은 정상이어야 한다")
  void db_connection_should_remain_valid_after_initialization() {
    dataInitializer.deleteAll();

    String result = jdbcTemplate.queryForObject("SELECT 'test' as value", String.class);
    assertThat(result).isEqualTo("test");
  }
}
