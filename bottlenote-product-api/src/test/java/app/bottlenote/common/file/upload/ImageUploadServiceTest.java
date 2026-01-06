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
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.upload.fixture.FakeAmazonS3;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
@DisplayName("[unit] [service] ImageUploadService")
class ImageUploadServiceTest {

  private static final Logger log = LoggerFactory.getLogger(ImageUploadServiceTest.class);
  private static final String BUCKET_NAME = "test-bucket";
  private static final String CLOUD_FRONT_URL = "https://cdn.example.com";
  private static final String AWS_URL = "https://" + BUCKET_NAME + ".s3.amazonaws.com/";
  private static final String UPLOAD_DATE = LocalDate.of(2024, 5, 1).format(ofPattern("yyyyMMdd"));
  private static final String FAKE_UUID = "ddd8d2d8-7b0c-47e9-91d0-d21251f891e8";

  private ImageUploadService imageUploadService;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeEach
  void setUp() {
    resourceLogRepository = new InMemoryResourceLogRepository();
    ResourceCommandService resourceCommandService =
        new ResourceCommandService(resourceLogRepository);
    imageUploadService =
        new ImageUploadService(
            resourceCommandService, new FakeAmazonS3(), BUCKET_NAME, CLOUD_FRONT_URL) {
          @Override
          public String getImageKey(String rootPath, Long index) {
            if (rootPath.startsWith(PATH_DELIMITER)) {
              rootPath = rootPath.substring(1);
            }
            if (rootPath.endsWith(PATH_DELIMITER)) {
              rootPath = rootPath.substring(0, rootPath.length() - 1);
            }
            String imageId = index + KEY_DELIMITER + FAKE_UUID + "." + EXTENSION;
            return rootPath + PATH_DELIMITER + UPLOAD_DATE + PATH_DELIMITER + imageId;
          }
        };
  }

  @Nested
  @DisplayName("PreSigned URL 생성 테스트")
  class PreSignedUrlTest {

    @Test
    @DisplayName("PreSignUrl을 생성할 수 있다")
    void test_1() {
      // given
      String imageKey = imageUploadService.getImageKey("review", 1L);

      // when
      String preSignUrl = imageUploadService.generatePreSignUrl(imageKey);

      // then
      log.info("PreSignUrl: {}", preSignUrl);
      assertNotNull(preSignUrl);
      assertEquals(AWS_URL + imageKey, preSignUrl);
    }

    @Test
    @DisplayName("업로드용 인증 URL을 생성할 수 있다")
    void test_2() {
      // given
      ImageUploadRequest request = new ImageUploadRequest("review", 2L);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);

      // then
      assertNotNull(response);
      assertEquals(request.uploadSize(), response.uploadSize());
      assertEquals(5, response.expiryTime());
      assertEquals(BUCKET_NAME, response.bucketName());

      for (Long index = 1L; index <= response.imageUploadInfo().size(); index++) {
        String imageKey = imageUploadService.getImageKey(request.rootPath(), index);
        String uploadUrlExpected = imageUploadService.generatePreSignUrl(imageKey);
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
      ImageUploadRequest request = new ImageUploadRequest("review", 1L);

      // when
      ImageUploadResponse response = imageUploadService.getPreSignUrl(request);

      // then
      assertNotNull(response);
      assertEquals(1, response.uploadSize());
      assertEquals(BUCKET_NAME, response.bucketName());

      ImageUploadItem item = response.imageUploadInfo().get(0);
      assertTrue(item.uploadUrl().startsWith(AWS_URL));
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
      String imageKey = imageUploadService.getImageKey("review", 1L);

      // when
      String viewUrl = imageUploadService.generateViewUrl(CLOUD_FRONT_URL, imageKey);

      // then
      log.info("ViewUrl: {}", viewUrl);
      assertNotNull(viewUrl);
      assertEquals(CLOUD_FRONT_URL + "/" + imageKey, viewUrl);
    }
  }

  @Nested
  @DisplayName("이미지 키 생성 테스트")
  class ImageKeyTest {

    @Test
    @DisplayName("이미지 루트 경로와 인덱스를 제공해 이미지 키를 생성할 수 있다")
    void test_1() {
      // given & when
      String imageKey = imageUploadService.getImageKey("review", 1L);
      String expected = "review/" + UPLOAD_DATE + "/1-" + FAKE_UUID + ".jpg";

      // then
      log.info("ImageKey: {}", imageKey);
      assertNotNull(imageKey);
      assertEquals(expected, imageKey);
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
