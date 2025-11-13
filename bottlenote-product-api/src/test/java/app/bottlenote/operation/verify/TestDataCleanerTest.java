package app.bottlenote.operation.verify;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.DataInitializer;
import app.bottlenote.operation.utils.TestContainersConfig;
import app.bottlenote.operation.utils.TestDataCleaner;
import org.junit.jupiter.api.AfterEach;
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
 * TestDataCleaner의 동작을 검증하는 테스트
 *
 * <p>DataInitializer로의 위임이 정상 동작하는지 확인
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Tag("operation")
@DisplayName("TestDataCleaner 검증 테스트")
class TestDataCleanerTest {

  @Autowired private TestDataCleaner testDataCleaner;
  @Autowired private DataInitializer dataInitializer;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    testDataCleaner.cleanAll();
  }

  @Test
  @DisplayName("TestDataCleaner가 Spring Bean으로 등록되어야 한다")
  void test_data_cleaner_should_be_registered_as_bean() {
    assertThat(testDataCleaner).isNotNull();
  }

  @Test
  @DisplayName("cleanAll()은 모든 테이블 데이터를 삭제해야 한다")
  void clean_all_should_truncate_all_tables() {
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    dataInitializer.refreshCache();

    jdbcTemplate.execute("INSERT INTO test_table VALUES (1, 'test')");

    Integer countBefore =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Integer.class);
    assertThat(countBefore).isEqualTo(1);

    testDataCleaner.cleanAll();

    Integer countAfter =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Integer.class);
    assertThat(countAfter).isZero();

    jdbcTemplate.execute("DROP TABLE IF EXISTS test_table");
  }

  @Test
  @DisplayName("cleanAll()을 여러 번 호출해도 문제없어야 한다")
  void clean_all_should_work_multiple_times() {
    testDataCleaner.cleanAll();
    testDataCleaner.cleanAll();
    testDataCleaner.cleanAll();

    String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
    assertThat(result).isEqualTo("1");
  }

  @Test
  @DisplayName("데이터 정리 후 DB 연결은 정상이어야 한다")
  void db_connection_should_be_valid_after_cleanup() {
    testDataCleaner.cleanAll();

    String result = jdbcTemplate.queryForObject("SELECT 'connected'", String.class);
    assertThat(result).isEqualTo("connected");
  }
}
