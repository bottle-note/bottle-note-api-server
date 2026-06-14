package app.bottlenote.common.file.upload;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.exception.FileException;
import app.bottlenote.common.file.exception.FileExceptionCode;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Tag("unit")
@DisplayName("[unit] [service] ImageUploadService")
class ImageUploadServiceTest {

  private static final Logger log = LoggerFactory.getLogger(ImageUploadServiceTest.class);
  private static final String BUCKET_NAME = "test-bucket";
  private static final String CLOUD_FRONT_URL = "https://cdn.example.com";
  private static final String UPLOAD_DATE = LocalDate.of(2024, 5, 1).format(ofPattern("yyyyMMdd"));
  private static final String FAKE_UUID = "ddd8d2d8-7b0c-47e9-91d0-d21251f891e8";

  private ImageUploadService imageUploadService;
  private S3Presigner s3Presigner;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeEach
  void setUp() {
    s3Presigner =
        S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
            .build();
    resourceLogRepository = new InMemoryResourceLogRepository();
    ResourceCommandService resourceCommandService =
        new ResourceCommandService(resourceLogRepository);
    imageUploadService =
        new ImageUploadService(resourceCommandService, s3Presigner, BUCKET_NAME, CLOUD_FRONT_URL) {
          @Override
          public String getImageKey(String rootPath, Long index, String contentType) {
            if (rootPath.startsWith(PATH_DELIMITER)) {
              rootPath = rootPath.substring(1);
            }
            if (rootPath.endsWith(PATH_DELIMITER)) {
              rootPath = rootPath.substring(0, rootPath.length() - 1);
            }
            String extension = ALLOWED_CONTENT_TYPES.get(contentType);
            if (extension == null) {
              throw new FileException(FileExceptionCode.UNSUPPORTED_CONTENT_TYPE);
            }
            String imageId = index + KEY_DELIMITER + FAKE_UUID + "." + extension;
            return rootPath + PATH_DELIMITER + UPLOAD_DATE + PATH_DELIMITER + imageId;
          }
        };
  }

  @AfterEach
  void closePresigner() {
    if (s3Presigner != null) {
      s3Presigner.close();
    }
  }

  @Nested
  @DisplayName("PreSigned URL 생성 테스트")
  class PreSignedUrlTest {

    @Test
    @DisplayName("PreSignUrl을 생성할 수 있다")
    void test_1() {
      // given
      String imageKey = imageUploadService.getImageKey("review", 1L, "image/jpeg");

      // when
      String preSignUrl = imageUploadService.generatePreSignUrl(imageKey, "image/jpeg");

      // then
      log.info("PreSignUrl: {}", preSignUrl);
      assertNotNull(preSignUrl);
      assertTrue(preSignUrl.startsWith("https://" + BUCKET_NAME + ".s3."));
      assertTrue(preSignUrl.contains(imageKey));
      assertTrue(preSignUrl.contains("X-Amz-Algorithm="));
    }

    @Test
    @DisplayName("업로드용 인증 URL을 생성할 수 있다")
    void test_2() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 2L, null);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);

      // then
      assertNotNull(response);
      assertEquals(request.uploadSize(), response.uploadSize());
      assertEquals(5, response.expiryTime());
      assertEquals(BUCKET_NAME, response.bucketName());

      for (Long index = 1L; index <= response.imageUploadInfo().size(); index++) {
        String imageKey = imageUploadService.getImageKey(request.rootPath(), index, "image/jpeg");
        String uploadUrlExpected = imageUploadService.generatePreSignUrl(imageKey, "image/jpeg");
        String viewUrlExpected = imageUploadService.generateViewUrl(CLOUD_FRONT_URL, imageKey);

        ImageUploadItem info = response.imageUploadInfo().get((int) (index - 1));

        log.info("[{}] ImageUploadItem: {}", index, info);
        assertEquals(index, info.order());
        assertEquals(uploadUrlExpected, info.uploadUrl());
        assertEquals(viewUrlExpected, info.viewUrl());
      }
    }

    @Test
    @DisplayName("단건 이미지 업로드 URL을 생성할 수 있다")
    void test_3() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 1L, null);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);

      // then
      assertNotNull(response);
      assertEquals(1, response.uploadSize());
      assertEquals(BUCKET_NAME, response.bucketName());

      ImageUploadItem item = response.imageUploadInfo().get(0);
      assertTrue(item.uploadUrl().startsWith("https://" + BUCKET_NAME + ".s3."));
      assertTrue(item.viewUrl().startsWith(CLOUD_FRONT_URL));
    }
  }

  @Nested
  @DisplayName("View URL 생성 테스트")
  class ViewUrlTest {

    @Test
    @DisplayName("조회용 URL을 생성할 수 있다")
    void test_1() {
      // given
      String imageKey = imageUploadService.getImageKey("review", 1L, "image/jpeg");

      // when
      String viewUrl = imageUploadService.generateViewUrl(CLOUD_FRONT_URL, imageKey);

      // then
      log.info("ViewUrl: {}", viewUrl);
      assertNotNull(viewUrl);
      assertEquals(CLOUD_FRONT_URL + "/" + imageKey, viewUrl);
    }

    @Test
    @DisplayName("CloudFront URL 끝에 slash가 있어도 조회용 URL에 중복 slash가 생기지 않는다")
    void generateViewUrl_whenCloudFrontUrlHasTrailingSlash_normalizesUrl() {
      // given
      String imageKey = imageUploadService.getImageKey("review", 1L, "image/jpeg");

      // when
      String viewUrl = imageUploadService.generateViewUrl(CLOUD_FRONT_URL + "/", imageKey);

      // then
      assertEquals(CLOUD_FRONT_URL + "/" + imageKey, viewUrl);
    }
  }

  @Nested
  @DisplayName("ResourceLog 저장 테스트")
  class ResourceLogTest {

    @Test
    @DisplayName("어드민 PreSign URL 생성은 응답 전 CREATED 로그를 저장한다")
    void getPreSignUrlForAdmin_savesCreatedLogsSynchronously() {
      // given
      Long adminId = 1L;
      ImageUploadRequest request = new ImageUploadRequest("review", 2L, null);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrlForAdmin(adminId, request);

      // then
      assertEquals(2, response.imageUploadInfo().size());
      assertEquals(2, resourceLogRepository.findByUserId(adminId).size());
      response
          .imageUploadInfo()
          .forEach(
              item ->
                  assertTrue(
                      resourceLogRepository
                          .findByResourceKey(item.viewUrl().substring(CLOUD_FRONT_URL.length() + 1))
                          .isPresent()));
    }
  }

  @Nested
  @DisplayName("이미지 키 생성 테스트")
  class ImageKeyTest {

    @Test
    @DisplayName("이미지 루트 경로와 인덱스를 제공해 이미지 키를 생성할 수 있다")
    void test_1() {
      // given & when
      String imageKey = imageUploadService.getImageKey("review", 1L, "image/jpeg");
      String expected = "review/" + UPLOAD_DATE + "/1-" + FAKE_UUID + ".jpg";

      // then
      log.info("ImageKey: {}", imageKey);
      assertNotNull(imageKey);
      assertEquals(expected, imageKey);
    }

    @Test
    @DisplayName("video/mp4 contentType으로 키 생성 시 확장자가 .mp4이다")
    void test_2() {
      // given & when
      String imageKey = imageUploadService.getImageKey("review", 1L, "video/mp4");

      // then
      log.info("ImageKey: {}", imageKey);
      assertTrue(imageKey.endsWith(".mp4"));
    }

    @Test
    @DisplayName("허용된 모든 contentType으로 키를 생성할 수 있다")
    void test_3() {
      // given
      Map<String, String> expectedExtensions =
          Map.of(
              "image/jpeg",
              ".jpg",
              "image/png",
              ".png",
              "image/webp",
              ".webp",
              "video/mp4",
              ".mp4");

      expectedExtensions.forEach(
          (contentType, extension) -> {
            // when
            String imageKey = imageUploadService.getImageKey("review", 1L, contentType);

            // then
            log.info("contentType: {} -> ImageKey: {}", contentType, imageKey);
            assertTrue(
                imageKey.endsWith(extension),
                "contentType " + contentType + "의 확장자는 " + extension + "이어야 한다");
          });
    }

    @Test
    @DisplayName("허용되지 않은 contentType으로 키 생성 시 예외가 발생한다")
    void test_4() {
      // given & when & then
      assertThrows(
          FileException.class,
          () -> imageUploadService.getImageKey("review", 1L, "application/zip"));
    }
  }

  @Nested
  @DisplayName("만료 시간 테스트")
  class ExpiryTimeTest {

    @Test
    @DisplayName("기본 만료 시간은 5분이다")
    void test_1() {
      // given
      Calendar expectedExpiryTime = Calendar.getInstance();
      expectedExpiryTime.add(Calendar.MINUTE, 5);

      // when
      Calendar actualExpiryTime = imageUploadService.getUploadExpiryTime(null);

      // then
      log.info("ExpiryTime: {}", actualExpiryTime);
      long diffInMillis =
          Math.abs(expectedExpiryTime.getTimeInMillis() - actualExpiryTime.getTimeInMillis());
      assertTrue(
          diffInMillis < TimeUnit.SECONDS.toMillis(1),
          "The difference should be less than 1 second");
    }

    @Test
    @DisplayName("최대 만료 시간은 10분이다")
    void test_2() {
      // given
      Calendar expectedExpiryTime = Calendar.getInstance();
      expectedExpiryTime.add(Calendar.MINUTE, 10);

      // when
      Calendar actualExpiryTime = imageUploadService.getUploadExpiryTime(10);

      // then
      long diffInMillis =
          Math.abs(expectedExpiryTime.getTimeInMillis() - actualExpiryTime.getTimeInMillis());
      assertTrue(
          diffInMillis < TimeUnit.SECONDS.toMillis(1),
          "The difference should be less than 1 second");
      assertThrows(FileException.class, () -> imageUploadService.getUploadExpiryTime(11));
    }
  }
}
