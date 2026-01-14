package app.bottlenote.common.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("unit")
@Testcontainers
@DisplayName("[unit] ImageUpload MinIO 기반 테스트")
class ImageUploadUnitTest {

  private static final Logger log = LoggerFactory.getLogger(ImageUploadUnitTest.class);
  private static final String MINIO_ACCESS_KEY = "minioadmin";
  private static final String MINIO_SECRET_KEY = "minioadmin";
  private static final String TEST_BUCKET = "test-bucket";
  private static final String CLOUD_FRONT_URL = "https://cdn.example.com";

  @Container
  static MinIOContainer minioContainer =
      new MinIOContainer("minio/minio:latest")
          .withUserName(MINIO_ACCESS_KEY)
          .withPassword(MINIO_SECRET_KEY);

  private static AmazonS3 amazonS3;
  private ImageUploadService imageUploadService;
  private ResourceCommandService resourceCommandService;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeAll
  static void setUpContainer() {
    amazonS3 =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(minioContainer.getS3URL(), "us-east-1"))
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
            .withPathStyleAccessEnabled(true)
            .build();

    if (!amazonS3.doesBucketExistV2(TEST_BUCKET)) {
      amazonS3.createBucket(TEST_BUCKET);
    }
  }

  @BeforeEach
  void setUp() {
    resourceLogRepository = new InMemoryResourceLogRepository();
    resourceCommandService = new ResourceCommandService(resourceLogRepository);
    imageUploadService =
        new ImageUploadService(resourceCommandService, amazonS3, TEST_BUCKET, CLOUD_FRONT_URL);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    resourceLogRepository.clear();
  }

  @Nested
  @DisplayName("PreSigned URL 생성 테스트")
  class PreSignedUrlTest {

    @Test
    @DisplayName("PreSigned URL 생성 시 MinIO에서 유효한 URL을 반환한다")
    void test_1() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);

      // then
      assertNotNull(response);
      assertEquals(1, response.uploadSize());
      assertEquals(TEST_BUCKET, response.bucketName());
      assertNotNull(response.imageUploadInfo().get(0).uploadUrl());
      assertTrue(response.imageUploadInfo().get(0).uploadUrl().contains(minioContainer.getS3URL()));

      log.info("PreSigned URL = {}", response.imageUploadInfo().get(0).uploadUrl());
    }

    @Test
    @DisplayName("PreSigned URL로 실제 파일 업로드가 가능하다")
    void test_2() throws Exception {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L);
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);
      String uploadUrl = response.imageUploadInfo().get(0).uploadUrl();
      byte[] testData = "test image content".getBytes();

      // when
      URL url = new URL(uploadUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      connection.setRequestProperty("Content-Type", "image/jpeg");
      connection.setRequestProperty("Content-Length", String.valueOf(testData.length));

      try (OutputStream os = connection.getOutputStream()) {
        os.write(testData);
      }
      int responseCode = connection.getResponseCode();
      connection.disconnect();

      // then
      assertEquals(200, responseCode);
      log.info("업로드 응답 코드 = {}", responseCode);
    }

    @Test
    @DisplayName("업로드된 파일이 MinIO에 존재한다")
    void test_3() throws Exception {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L);
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);
      String uploadUrl = response.imageUploadInfo().get(0).uploadUrl();
      String viewUrl = response.imageUploadInfo().get(0).viewUrl();
      String imageKey = viewUrl.substring(CLOUD_FRONT_URL.length() + 1);
      byte[] testData = "test image content".getBytes();

      URL url = new URL(uploadUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      connection.setRequestProperty("Content-Type", "image/jpeg");
      try (OutputStream os = connection.getOutputStream()) {
        os.write(testData);
      }
      connection.getResponseCode();
      connection.disconnect();

      // when
      boolean exists = amazonS3.doesObjectExist(TEST_BUCKET, imageKey);

      // then
      assertTrue(exists);
      log.info("업로드된 객체 키 = {}, 존재 여부 = {}", imageKey, exists);
    }
  }

  @Nested
  @DisplayName("리소스 로그 저장 테스트")
  class ResourceLogTest {

    @Test
    @DisplayName("로그인 사용자가 PreSigned URL 생성 시 CREATED 이벤트 로그가 저장된다")
    void test_1() {
      // given
      Long userId = 1L;
      SecurityContextHolder.getContext()
          .setAuthentication(new TestingAuthenticationToken(userId.toString(), null));
      ImageUploadRequest request = new ImageUploadRequest("review", 2L);

      // when
      imageUploadService.getPreSignUrl(request);

      // then
      List<ResourceLog> logs = resourceLogRepository.findByUserId(userId);
      assertEquals(2, logs.size());
      assertEquals(ResourceEventType.CREATED, logs.get(0).getEventType());
      assertEquals("IMAGE", logs.get(0).getResourceType());

      log.info("저장된 로그 수 = {}", logs.size());
    }

    @Test
    @DisplayName("비로그인 사용자가 PreSigned URL 생성 시 로그가 저장되지 않는다")
    void test_2() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 2L);

      // when
      imageUploadService.getPreSignUrl(request);

      // then
      List<ResourceLog> logs = resourceLogRepository.findAll();
      assertEquals(0, logs.size());

      log.info("저장된 로그 수 = {}", logs.size());
    }
  }

  static class InMemoryResourceLogRepository implements ResourceLogRepository {

    private final Map<Long, ResourceLog> database = new HashMap<>();

    @Override
    public ResourceLog save(ResourceLog resourceLog) {
      Long id = (Long) ReflectionTestUtils.getField(resourceLog, "id");
      if (id == null) {
        id = database.size() + 1L;
        ReflectionTestUtils.setField(resourceLog, "id", id);
      }
      database.put(id, resourceLog);
      return resourceLog;
    }

    @Override
    public Optional<ResourceLog> findById(Long id) {
      return Optional.ofNullable(database.get(id));
    }

    @Override
    public List<ResourceLog> findByResourceKey(String resourceKey) {
      return database.values().stream()
          .filter(log -> log.getResourceKey().equals(resourceKey))
          .toList();
    }

    @Override
    public List<ResourceLog> findByUserId(Long userId) {
      return database.values().stream().filter(log -> log.getUserId().equals(userId)).toList();
    }

    @Override
    public List<ResourceLog> findByEventTypeAndCreateAtBefore(
        ResourceEventType eventType, LocalDateTime dateTime) {
      return database.values().stream()
          .filter(log -> log.getEventType() == eventType)
          .filter(log -> log.getCreateAt() == null || log.getCreateAt().isBefore(dateTime))
          .toList();
    }

    @Override
    public List<ResourceLog> findByReferenceIdAndReferenceType(
        Long referenceId, String referenceType) {
      return database.values().stream()
          .filter(log -> referenceId.equals(log.getReferenceId()))
          .filter(log -> referenceType.equals(log.getReferenceType()))
          .toList();
    }

    @Override
    public Optional<ResourceLog> findLatestByResourceKey(String resourceKey) {
      return database.values().stream()
          .filter(log -> log.getResourceKey().equals(resourceKey))
          .max(Comparator.comparing(ResourceLog::getId));
    }

    @Override
    public boolean existsByResourceKeyAndReferenceIdAndEventType(
        String resourceKey, Long referenceId, ResourceEventType eventType) {
      return database.values().stream()
          .anyMatch(
              log ->
                  log.getResourceKey().equals(resourceKey)
                      && referenceId.equals(log.getReferenceId())
                      && log.getEventType() == eventType);
    }

    @Override
    public void delete(ResourceLog resourceLog) {
      database.remove(resourceLog.getId());
    }

    public void clear() {
      database.clear();
    }

    public List<ResourceLog> findAll() {
      return List.copyOf(database.values());
    }
  }
}
