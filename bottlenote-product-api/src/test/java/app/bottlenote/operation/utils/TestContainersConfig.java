package app.bottlenote.operation.utils;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers 설정을 관리하는 Spring Bean 기반 Configuration
 *
 * <p>Spring Boot 3.1+ @ServiceConnection을 활용하여 컨테이너 자동 연결
 */
@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("resource")
public class TestContainersConfig {

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

  /** Redis 컨테이너를 Spring Bean으로 등록 @ServiceConnection이 자동으로 Redis 설정을 처리 */
  @Bean
  @ServiceConnection
  RedisContainer redisContainer() {
    return new RedisContainer(DockerImageName.parse("redis:7.0.12")).withReuse(true);
  }
}
