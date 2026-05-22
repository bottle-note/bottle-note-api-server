package app.bottlenote.common.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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

  private static S3Client s3Client;
  private static S3Presigner s3Presigner;
  private ImageUploadService imageUploadService;
  private ResourceCommandService resourceCommandService;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeAll
  static void setUpContainer() {
    StaticCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY));
    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(true).build();
    URI endpoint = URI.create(minioContainer.getS3URL());

    s3Client =
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .build();
    s3Presigner =
        S3Presigner.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(s3Configuration)
            .build();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
  }

  @AfterAll
  static void closeClients() {
    if (s3Client != null) {
      s3Client.close();
    }
    if (s3Presigner != null) {
      s3Presigner.close();
    }
  }

  @BeforeEach
  void setUp() {
    resourceLogRepository = new InMemoryResourceLogRepository();
    resourceCommandService = new ResourceCommandService(resourceLogRepository);
    imageUploadService =
        new ImageUploadService(resourceCommandService, s3Presigner, TEST_BUCKET, CLOUD_FRONT_URL);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    resourceLogRepository.clear();
  }

  private int upload(String uploadUrl, byte[] data, String contentType) throws Exception {
    URL url = new URL(uploadUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Content-Type", contentType);
    connection.setRequestProperty("Content-Length", String.valueOf(data.length));

    try (OutputStream os = connection.getOutputStream()) {
      os.write(data);
    }
    int responseCode = connection.getResponseCode();
    connection.disconnect();
    return responseCode;
  }

  @Nested
  @DisplayName("PreSigned URL 생성 테스트")
  class PreSignedUrlTest {

    @Test
    @DisplayName("PreSigned URL 생성 시 MinIO에서 유효한 URL을 반환한다")
    void test_1() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, null);

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
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, null);
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
    @DisplayName("presigned URL 발급 시 서명된 contentType과 다른 contentType으로 PUT 하는 경우")
    void test_2_content_type_mismatch() throws Exception {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, "image/png");
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);
      String uploadUrl = response.imageUploadInfo().get(0).uploadUrl();
      byte[] testData = "test image content".getBytes(StandardCharsets.UTF_8);

      // when
      int responseCode = upload(uploadUrl, testData, "image/jpeg");

      // then
      assertNotEquals(200, responseCode);
    }

    @Test
    @DisplayName("업로드된 파일이 MinIO에 존재한다")
    void test_3() throws Exception {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, null);
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
      s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET).key(imageKey).build());

      // then
      log.info("업로드된 객체 키 = {}", imageKey);
    }

    @Test
    @DisplayName("업로드된 파일 내용을 MinIO에서 조회할 수 있다")
    void test_4() throws Exception {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, null);
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
      String content;
      try (ResponseInputStream<GetObjectResponse> responseInputStream =
          s3Client.getObject(
              GetObjectRequest.builder().bucket(TEST_BUCKET).key(imageKey).build())) {
        content = new String(responseInputStream.readAllBytes(), StandardCharsets.UTF_8);
      }

      // then
      assertEquals("test image content", content);
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
      ImageUploadRequest request = new ImageUploadRequest("review", 2L, null);

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
      ImageUploadRequest request = new ImageUploadRequest("review", 2L, null);

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
    private final Map<String, Long> resourceKeyIndex = new HashMap<>();

    @Override
    public ResourceLog save(ResourceLog resourceLog) {
      Long id = (Long) ReflectionTestUtils.getField(resourceLog, "id");
      if (id == null) {
        id = database.size() + 1L;
        ReflectionTestUtils.setField(resourceLog, "id", id);
      }
      database.put(id, resourceLog);
      resourceKeyIndex.put(resourceLog.getResourceKey(), id);
      return resourceLog;
    }

    @Override
    public Optional<ResourceLog> findById(Long id) {
      return Optional.ofNullable(database.get(id));
    }

    @Override
    public Optional<ResourceLog> findByResourceKey(String resourceKey) {
      Long id = resourceKeyIndex.get(resourceKey);
      if (id == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(database.get(id));
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
    public void delete(ResourceLog resourceLog) {
      resourceKeyIndex.remove(resourceLog.getResourceKey());
      database.remove(resourceLog.getId());
    }

    public void clear() {
      database.clear();
      resourceKeyIndex.clear();
    }

    public List<ResourceLog> findAll() {
      return List.copyOf(database.values());
    }
  }
}
