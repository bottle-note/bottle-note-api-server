package app.bottlenote.common.file.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.common.file.constant.ImageUploadStatus;
import app.bottlenote.common.file.dto.request.ImageUploadLogRequest;
import app.bottlenote.common.file.dto.response.ImageUploadLogItem;
import app.bottlenote.common.file.dto.response.ImageUploadLogResponse;
import app.bottlenote.common.file.service.ImageUploadLogService;
import app.bottlenote.common.file.upload.fixture.InMemoryImageUploadLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("[unit] [service] ImageUploadLog")
class ImageUploadLogServiceTest {

  private static final Logger log = LoggerFactory.getLogger(ImageUploadLogServiceTest.class);
  private ImageUploadLogService imageUploadLogService;
  private InMemoryImageUploadLogRepository imageUploadLogRepository;

  @BeforeEach
  void setUp() {
    imageUploadLogRepository = new InMemoryImageUploadLogRepository();
    imageUploadLogService = new ImageUploadLogService(imageUploadLogRepository);
  }

  private ImageUploadLogRequest createRequest(Long userId, String imageKey) {
    return ImageUploadLogRequest.builder()
        .userId(userId)
        .imageKey(imageKey)
        .viewUrl("https://cdn.example.com/" + imageKey)
        .rootPath("review")
        .bucketName("test-bucket")
        .originalFileName("test.jpg")
        .contentType("image/jpeg")
        .contentLength(1024L)
        .build();
  }

  @Nested
  @DisplayName("이미지 로그 저장 테스트")
  class SaveAsyncTest {

    @Test
    @DisplayName("이미지 로그 요청을 저장할 때 PENDING 상태로 저장된다")
    void test_1() {
      // given
      ImageUploadLogRequest request = createRequest(1L, "review/20251231/1-uuid.jpg");

      // when
      CompletableFuture<ImageUploadLogResponse> future = imageUploadLogService.saveAsync(request);
      ImageUploadLogResponse response = future.join();

      // then
      assertNotNull(response);
      assertEquals(1L, response.id());
      assertEquals(1L, response.userId());
      assertEquals("review/20251231/1-uuid.jpg", response.imageKey());
      assertEquals(ImageUploadStatus.PENDING, response.status());

      log.info("저장된 응답 = {}", response);
    }

    @Test
    @DisplayName("여러 이미지 로그를 저장할 때 각각 다른 ID로 저장된다")
    void test_2() {
      // given
      ImageUploadLogRequest request1 = createRequest(1L, "review/20251231/1-uuid1.jpg");
      ImageUploadLogRequest request2 = createRequest(1L, "review/20251231/2-uuid2.jpg");

      // when
      ImageUploadLogResponse response1 = imageUploadLogService.saveAsync(request1).join();
      ImageUploadLogResponse response2 = imageUploadLogService.saveAsync(request2).join();

      // then
      assertEquals(1L, response1.id());
      assertEquals(2L, response2.id());
      assertEquals(2, imageUploadLogRepository.findAll().size());

      log.info("첫 번째 저장 = {}", response1);
      log.info("두 번째 저장 = {}", response2);
    }
  }

  @Nested
  @DisplayName("이미지 로그 조회 테스트")
  class FindByImageKeyTest {

    @Test
    @DisplayName("imageKey로 조회할 때 저장된 로그를 반환한다")
    void test_1() {
      // given
      String imageKey = "review/20251231/1-uuid.jpg";
      imageUploadLogService.saveAsync(createRequest(1L, imageKey)).join();

      // when
      Optional<ImageUploadLogResponse> result = imageUploadLogService.findByImageKey(imageKey);

      // then
      assertTrue(result.isPresent());
      assertEquals(imageKey, result.get().imageKey());

      log.info("조회 결과 = {}", result.get());
    }

    @Test
    @DisplayName("존재하지 않는 imageKey로 조회할 때 빈 결과를 반환한다")
    void test_2() {
      // given
      String imageKey = "non-existent-key.jpg";

      // when
      Optional<ImageUploadLogResponse> result = imageUploadLogService.findByImageKey(imageKey);

      // then
      assertTrue(result.isEmpty());

      log.info("조회 결과 = {}", result);
    }
  }

  @Nested
  @DisplayName("미확정 로그 목록 조회 테스트")
  class FindUnconfirmedLogsTest {

    @Test
    @DisplayName("상태와 날짜 기준으로 미확정 로그 목록을 조회할 때 조건에 맞는 목록을 반환한다")
    void test_1() {
      // given
      imageUploadLogService.saveAsync(createRequest(1L, "review/20251231/1-uuid1.jpg")).join();
      imageUploadLogService.saveAsync(createRequest(2L, "review/20251231/2-uuid2.jpg")).join();

      // createAt 설정 (과거 날짜로)
      imageUploadLogRepository
          .findById(1L)
          .ifPresent(
              uploadLog ->
                  ReflectionTestUtils.setField(
                      uploadLog, "createAt", LocalDateTime.now().minusDays(7)));
      imageUploadLogRepository
          .findById(2L)
          .ifPresent(
              uploadLog ->
                  ReflectionTestUtils.setField(
                      uploadLog, "createAt", LocalDateTime.now().minusDays(3)));

      // when
      List<ImageUploadLogItem> result =
          imageUploadLogService.findUnconfirmedLogs(
              ImageUploadStatus.PENDING, LocalDateTime.now().minusDays(1));

      // then
      assertEquals(2, result.size());

      log.info("조회된 미확정 로그 수 = {}", result.size());
    }

    @Test
    @DisplayName("CONFIRMED 상태의 로그는 PENDING 조회 시 제외된다")
    void test_2() {
      // given
      imageUploadLogService.saveAsync(createRequest(1L, "review/20251231/1-uuid1.jpg")).join();
      imageUploadLogRepository
          .findById(1L)
          .ifPresent(
              uploadLog -> {
                ReflectionTestUtils.setField(
                    uploadLog, "createAt", LocalDateTime.now().minusDays(7));
                uploadLog.confirm(100L, "REVIEW");
              });

      // when
      List<ImageUploadLogItem> result =
          imageUploadLogService.findUnconfirmedLogs(ImageUploadStatus.PENDING, LocalDateTime.now());

      // then
      assertEquals(0, result.size());

      log.info("조회된 미확정 로그 수 = {}", result.size());
    }
  }

  @Nested
  @DisplayName("이미지 상태 확정 테스트")
  class ConfirmAsyncTest {

    @Test
    @DisplayName("이미지 상태를 확정할 때 CONFIRMED 상태로 변경된다")
    void test_1() {
      // given
      String imageKey = "review/20251231/1-uuid.jpg";
      imageUploadLogService.saveAsync(createRequest(1L, imageKey)).join();

      // when
      CompletableFuture<Optional<ImageUploadLogResponse>> future =
          imageUploadLogService.confirmAsync(imageKey, 100L, "REVIEW");
      Optional<ImageUploadLogResponse> result = future.join();

      // then
      assertTrue(result.isPresent());
      assertEquals(ImageUploadStatus.CONFIRMED, result.get().status());
      assertEquals(100L, result.get().referenceId());
      assertEquals("REVIEW", result.get().referenceType());
      assertNotNull(result.get().confirmedAt());

      log.info("확정 결과 = {}", result.get());
    }

    @Test
    @DisplayName("존재하지 않는 imageKey로 확정 요청할 때 빈 결과를 반환한다")
    void test_2() {
      // given
      String imageKey = "non-existent-key.jpg";

      // when
      CompletableFuture<Optional<ImageUploadLogResponse>> future =
          imageUploadLogService.confirmAsync(imageKey, 100L, "REVIEW");
      Optional<ImageUploadLogResponse> result = future.join();

      // then
      assertTrue(result.isEmpty());

      log.info("확정 결과 = {}", result);
    }
  }
}
