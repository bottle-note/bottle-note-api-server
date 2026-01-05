package app.bottlenote.operation.utils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MinIOContainer;
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

  /** MySQL 컨테이너를 Spring Bean으로 등록합니다. @ServiceConnection이 자동으로 DataSource 설정을 처리합니다. */
  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
        .withReuse(true)
        .withDatabaseName("bottlenote")
        .withUsername("root")
        .withPassword("root")
        .withInitScripts(
            "storage/mysql/init/00-init-config-table.sql",
            "storage/mysql/init/01-init-core-table.sql");
  }

  /** Redis 컨테이너를 Spring Bean으로 등록합니다. @ServiceConnection이 자동으로 Redis 설정을 처리합니다. */
  @Bean
  @ServiceConnection
  RedisContainer redisContainer() {
    return new RedisContainer(DockerImageName.parse("redis:7.0.12")).withReuse(true);
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
}
