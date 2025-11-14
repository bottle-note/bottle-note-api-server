package app.bottlenote;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.MySQLContainer;

@Tag("integration")
@DisplayName("[integration] [service] UserCommandService")
class ApplicationContextStartupIntegrationTest extends IntegrationTestSupport {

  @Autowired private MySQLContainer<?> mysqlContainer;
  @Autowired private RedisContainer redisContainer;

  @Test
  @DisplayName("컨텍스트 로드 확인 테스트")
  void contextLoads() {
    // MySQL Container 정보
    log.info("=== MySQL Container ===");
    log.info("Image: {}", mysqlContainer.getDockerImageName());
    log.info("Container ID: {}", mysqlContainer.getContainerId());
    log.info("Host: {}", mysqlContainer.getHost());
    log.info("Port: {}", mysqlContainer.getFirstMappedPort());
    log.info("Database: {}", mysqlContainer.getDatabaseName());
    log.info("Running: {}", mysqlContainer.isRunning());

    // Redis Container 정보
    log.info("=== Redis Container ===");
    log.info("Image: {}", redisContainer.getDockerImageName());
    log.info("Container ID: {}", redisContainer.getContainerId());
    log.info("Host: {}", redisContainer.getHost());
    log.info("Port: {}", redisContainer.getFirstMappedPort());
    log.info("Running: {}", redisContainer.isRunning());

    // 검증
    assertThat(mysqlContainer.isRunning()).isTrue();
    assertThat(redisContainer.isRunning()).isTrue();
    log.info("✅ Context loaded successfully - All containers running");
  }
}
