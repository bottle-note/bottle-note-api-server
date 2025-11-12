package app.bottlenote.operation.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers 설정을 관리하는 Spring Bean 기반 Configuration
 *
 * <p>Spring Boot 3.1+ @ServiceConnection을 활용하여 컨테이너 자동 연결
 *
 * <p>Redis는 GenericContainer 사용으로 @DynamicPropertySource로 수동 연결
 */
@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("resource")
public class TestContainersConfig {

  private static final GenericContainer<?> REDIS_CONTAINER =
      new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
          .withExposedPorts(6379)
          .withReuse(true);

  static {
    REDIS_CONTAINER.start();
  }

  /** MySQL 컨테이너를 Spring Bean으로 등록 @ServiceConnection이 자동으로 DataSource 설정을 처리 */
  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
        .withReuse(true)
        .withDatabaseName("bottlenote")
        .withUsername("root")
        .withPassword("root");
  }

  /** Redis 컨테이너를 Spring Bean으로 등록 */
  @Bean
  GenericContainer<?> redisContainer() {
    return REDIS_CONTAINER;
  }

  /** Redis 컨테이너의 동적 포트를 Spring 설정에 주입 */
  @DynamicPropertySource
  static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
  }
}
