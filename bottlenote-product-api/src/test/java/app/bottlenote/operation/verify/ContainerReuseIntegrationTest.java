package app.bottlenote.operation.verify;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.operation.utils.TestContainersConfig;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

/**
 * 컨테이너 재사용 동작을 검증하는 테스트
 *
 * <p>여러 테스트에서 동일한 컨테이너를 재사용하는지 확인
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Tag("operation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("컨테이너 재사용 검증 테스트")
class ContainerReuseIntegrationTest {

  private static String firstMysqlContainerId;
  private static String firstRedisContainerId;

  @Autowired private MySQLContainer<?> mysqlContainer;
  @Autowired private RedisContainer redisContainer;

  @Test
  @Order(1)
  @DisplayName("첫 번째 테스트에서 컨테이너 ID를 기록해야 한다")
  void first_test_should_record_container_ids() {
    assertThat(mysqlContainer.isRunning()).isTrue();
    assertThat(redisContainer.isRunning()).isTrue();

    firstMysqlContainerId = mysqlContainer.getContainerId();
    firstRedisContainerId = redisContainer.getContainerId();

    assertThat(firstMysqlContainerId).isNotNull();
    assertThat(firstRedisContainerId).isNotNull();
  }

  @Test
  @Order(2)
  @DisplayName("두 번째 테스트에서 같은 컨테이너 ID를 사용해야 한다")
  void second_test_should_reuse_same_containers() {
    assertThat(mysqlContainer.getContainerId()).isEqualTo(firstMysqlContainerId);
    assertThat(redisContainer.getContainerId()).isEqualTo(firstRedisContainerId);
  }

  @Test
  @Order(3)
  @DisplayName("컨테이너 재사용 설정이 활성화되어야 한다")
  void containers_should_have_reuse_flag_enabled() {
    assertThat(mysqlContainer.isShouldBeReused()).isTrue();
    assertThat(redisContainer.isShouldBeReused()).isTrue();
  }

  @Test
  @Order(4)
  @DisplayName("@Import를 통한 컴포지션 패턴이 정상 동작해야 한다")
  void composition_pattern_with_import_should_work() {
    assertThat(mysqlContainer).isNotNull();
    assertThat(redisContainer).isNotNull();
    assertThat(mysqlContainer.isRunning()).isTrue();
    assertThat(redisContainer.isRunning()).isTrue();
  }
}
