package app.bottlenote.operation.verify;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.operation.utils.TestContainersConfig;
import com.redis.testcontainers.RedisContainer;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

/**
 * TestContainersConfig의 동작을 검증하는 테스트
 *
 * <p>IntegrationTestSupport를 상속받지 않고 필요한 어노테이션만 직접 사용하는 순수 테스트
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("TestContainersConfig 검증 테스트")
class TestContainersConfigTest {

  @Autowired private MySQLContainer<?> mysqlContainer;
  @Autowired private RedisContainer redisContainer;
  @Autowired private DataSource dataSource;
  @Autowired private RedisConnectionFactory redisConnectionFactory;

  @Test
  @DisplayName("MySQL 컨테이너가 Bean으로 등록되고 실행 중이어야 한다")
  void mysql_container_should_be_running() {
    assertThat(mysqlContainer).isNotNull();
    assertThat(mysqlContainer.isRunning()).isTrue();
    assertThat(mysqlContainer.getDatabaseName()).isEqualTo("bottlenote");
  }

  @Test
  @DisplayName("Redis 컨테이너가 Bean으로 등록되고 실행 중이어야 한다")
  void redis_container_should_be_running() {
    assertThat(redisContainer).isNotNull();
    assertThat(redisContainer.isRunning()).isTrue();
  }

  @Test
  @DisplayName("@ServiceConnection이 DataSource를 MySQL 컨테이너에 자동 연결해야 한다")
  void service_connection_should_configure_datasource() throws Exception {
    assertThat(dataSource).isNotNull();

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    String result = jdbcTemplate.queryForObject("SELECT 1", String.class);

    assertThat(result).isEqualTo("1");
  }

  @Test
  @DisplayName("@ServiceConnection이 RedisConnectionFactory를 Redis 컨테이너에 자동 연결해야 한다")
  void service_connection_should_configure_redis() {
    assertThat(redisConnectionFactory).isNotNull();

    redisConnectionFactory
        .getConnection()
        .serverCommands()
        .setConfig("notify-keyspace-events", "Ex");

    String response = redisConnectionFactory.getConnection().ping();
    assertThat(response).isEqualTo("PONG");
  }

  @Test
  @DisplayName("컨테이너 재사용 설정이 활성화되어야 한다")
  void containers_should_have_reuse_enabled() {
    assertThat(mysqlContainer.isShouldBeReused()).isTrue();
    assertThat(redisContainer.isShouldBeReused()).isTrue();
  }
}
