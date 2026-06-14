package app.bottlenote.operation.utils;

import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import com.redis.testcontainers.RedisContainer;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * TestContainers 설정을 관리하는 Spring Bean 기반 Configuration
 *
 * <p>Spring Boot 3.1+ @ServiceConnection을 활용하여 컨테이너 자동 연결
 */
@TestConfiguration(proxyBeanMethods = false)
@SuppressWarnings("resource")
public class TestContainersConfig {

  private static final String DEFAULT_DB_NAME = "bottlenote";

  /** MySQL 컨테이너를 Spring Bean으로 등록합니다. @ServiceConnection이 자동으로 DataSource 설정을 처리합니다. */
  @Bean
  @ServiceConnection
  MySQLContainer<?> mysqlContainer() {
    String dbName = System.getProperty("testcontainers.db.name", DEFAULT_DB_NAME);
    return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
        .withReuse(true)
        .withDatabaseName(dbName)
        .withUsername("root")
        .withPassword("root");
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

  /** MinIO에 연결하는 S3Client를 등록합니다. */
  @Bean("testS3Client")
  @Primary
  S3Client s3Client(MinIOContainer minioContainer) {
    S3Client s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(minioContainer.getS3URL()))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();

    createBucketIfAbsent(s3Client);

    return s3Client;
  }

  /** MinIO에 연결하는 S3Presigner를 등록합니다. */
  @Bean("testS3Presigner")
  @Primary
  S3Presigner s3Presigner(MinIOContainer minioContainer) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(minioContainer.getS3URL()))
        .region(Region.US_EAST_1)
        .credentialsProvider(credentialsProvider())
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private StaticCredentialsProvider credentialsProvider() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY));
  }

  private void createBucketIfAbsent(S3Client s3Client) {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(TEST_BUCKET).build());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
        return;
      }
      throw exception;
    }
  }

  @Bean("testImageUploadService")
  @Primary
  ImageUploadService imageUploadService(
      ResourceCommandService resourceCommandService,
      S3Presigner s3Presigner,
      @Value("${amazon.aws.cloudFrontUrl}") String cloudFrontUrl) {
    return new ImageUploadService(resourceCommandService, s3Presigner, TEST_BUCKET, cloudFrontUrl);
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
