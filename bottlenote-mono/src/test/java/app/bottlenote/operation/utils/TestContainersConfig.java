package app.bottlenote.operation.utils;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.redis.testcontainers.RedisContainer;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers 설정. 공유 컨테이너 + fork별 DB 격리 패턴.
 *
 * <p>MySQL 컨테이너는 모든 Gradle test fork가 공유하고, fork마다 별도의 DB(스키마)를 만들어 격리한다. fork id는 Gradle이 주입하는
 * {@code org.gradle.test.worker} 시스템 프로퍼티로 결정된다.
 */
@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("resource")
public class TestContainersConfig {

  private static final String DEFAULT_DB_NAME = "bottlenote";
  private static final String BOOTSTRAP_DB_NAME = "bottlenote_bootstrap";

  private static String workerId() {
    return System.getProperty("org.gradle.test.worker", "0");
  }

  private static String forkDbName() {
    String base = System.getProperty("testcontainers.db.name", DEFAULT_DB_NAME);
    return base + "_w" + workerId();
  }

  /**
   * MySQL 컨테이너를 Spring Bean으로 등록합니다.
   *
   * <p>모든 fork가 동일한 부트스트랩 DB 이름으로 컨테이너를 정의하여 Testcontainers reuse 해시를 일치시킨다. 실제 fork별 DB는 {@link
   * #dataSource(MySQLContainer)}에서 동적으로 생성된다.
   */
  @Bean
  MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
        .withReuse(true)
        .withDatabaseName(BOOTSTRAP_DB_NAME)
        .withUsername("root")
        .withPassword("root");
  }

  /**
   * fork별 독립 DB로 라우팅되는 DataSource.
   *
   * <p>컨테이너 시작 후 fork 전용 DB를 멱등 생성하고, JDBC URL을 fork DB로 교체해 반환한다. Liquibase와 JPA는 이 DataSource를 통해
   * 자동으로 fork DB에 마이그레이션·검증을 수행한다.
   */
  @Bean
  @Primary
  DataSource dataSource(MySQLContainer<?> container) {
    container.start();
    String forkDb = forkDbName();
    try (Connection conn = container.createConnection("");
        Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE DATABASE IF NOT EXISTS `" + forkDb + "`");
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to create fork database: " + forkDb, e);
    }
    String forkUrl = container.getJdbcUrl().replaceFirst("/" + BOOTSTRAP_DB_NAME, "/" + forkDb);
    return DataSourceBuilder.create()
        .type(HikariDataSource.class)
        .url(forkUrl)
        .username(container.getUsername())
        .password(container.getPassword())
        .driverClassName(container.getDriverClassName())
        .build();
  }

  /** Redis 컨테이너를 Spring Bean으로 등록합니다. @ServiceConnection이 자동으로 Redis 설정을 처리합니다. */
  @Bean
  @ServiceConnection
  RedisContainer redisContainer() {
    return new RedisContainer(DockerImageName.parse("redis:7.0.12"));
  }

  /** 테스트용 Fake RestTemplate 빈. webhookRestTemplate을 대체합니다. */
  @Bean
  @Primary
  FakeWebhookRestTemplate webhookRestTemplate() {
    return new FakeWebhookRestTemplate();
  }

  private static final String MINIO_IMAGE = "minio/minio:latest";
  private static final String MINIO_ACCESS_KEY = "minioadmin";
  private static final String MINIO_SECRET_KEY = "minioadmin";
  private static final String TEST_BUCKET = "test-bucket";

  /** MinIO 컨테이너를 Spring Bean으로 등록합니다. */
  @Bean
  MinIOContainer minioContainer() {
    MinIOContainer container =
        new MinIOContainer(MINIO_IMAGE)
            .withUserName(MINIO_ACCESS_KEY)
            .withPassword(MINIO_SECRET_KEY);
    container.start();
    return container;
  }

  /** MinIO에 연결하는 AmazonS3 클라이언트를 등록합니다. */
  @Bean
  @Primary
  AmazonS3 amazonS3(MinIOContainer minioContainer) {
    AmazonS3 s3Client =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(minioContainer.getS3URL(), "us-east-1"))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
            .withPathStyleAccessEnabled(true)
            .build();

    if (!s3Client.doesBucketExistV2(TEST_BUCKET)) {
      s3Client.createBucket(TEST_BUCKET);
    }

    return s3Client;
  }

  public static String getTestBucket() {
    return TEST_BUCKET;
  }

  // 외부 비속어 필터 API 대신 Fake 구현체 사용
  @Bean
  @Primary
  ProfanityClient fakeProfanityClient() {
    return new ProfanityClient() {
      @Override
      public ProfanityResponse requestVerificationProfanity(String text) {
        return ProfanityResponse.builder()
            .trackingId(UUID.randomUUID().toString())
            .status(new ProfanityResponse.Status(200, "OK", "Fake", null))
            .detected(Collections.emptyList())
            .filtered(text)
            .elapsed("0.0")
            .build();
      }

      @Override
      public String getFilteredText(String text) {
        return text == null ? "" : text;
      }

      @Override
      public String filter(String content) {
        return content == null || content.isBlank() ? "" : content;
      }

      @Override
      public void validateProfanity(String text) {
        // 테스트 환경에서는 비속어 검증 생략
      }
    };
  }
}
