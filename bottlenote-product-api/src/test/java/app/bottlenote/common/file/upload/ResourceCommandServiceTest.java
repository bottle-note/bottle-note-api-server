package app.bottlenote.common.file.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.dto.response.ResourceLogItem;
import app.bottlenote.common.file.dto.response.ResourceLogResponse;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
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
@DisplayName("[unit] [service] ResourceCommandService")
class ResourceCommandServiceTest {

  private static final Logger log = LoggerFactory.getLogger(ResourceCommandServiceTest.class);
  private ResourceCommandService resourceCommandService;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeEach
  void setUp() {
    resourceLogRepository = new InMemoryResourceLogRepository();
    resourceCommandService = new ResourceCommandService(resourceLogRepository);
  }

  private ResourceLogRequest createRequest(Long userId, String resourceKey) {
    return ResourceLogRequest.builder()
        .userId(userId)
        .resourceKey(resourceKey)
        .viewUrl("https://cdn.example.com/" + resourceKey)
        .rootPath("review")
        .bucketName("test-bucket")
        .build();
  }

  @Nested
  @DisplayName("이미지 리소스 생성 로그 저장 테스트")
  class SaveImageResourceCreatedTest {

    @Test
    @DisplayName("이미지 리소스 생성 요청을 저장할 때 CREATED 이벤트로 저장된다")
    void test_1() {
      // given
      ResourceLogRequest request = createRequest(1L, "review/20251231/1-uuid.jpg");

      // when
      CompletableFuture<ResourceLogResponse> future =
          resourceCommandService.saveImageResourceCreated(request);
      ResourceLogResponse response = future.join();

      // then
      assertNotNull(response);
      assertEquals(1L, response.id());
      assertEquals(1L, response.userId());
      assertEquals("review/20251231/1-uuid.jpg", response.resourceKey());
      assertEquals("IMAGE", response.resourceType());
      assertEquals(ResourceEventType.CREATED, response.eventType());

      log.info("저장된 응답 = {}", response);
    }

    @Test
    @DisplayName("여러 이미지 리소스 생성 시 각각 다른 ID로 저장된다")
    void test_2() {
      // given
      ResourceLogRequest request1 = createRequest(1L, "review/20251231/1-uuid1.jpg");
      ResourceLogRequest request2 = createRequest(1L, "review/20251231/2-uuid2.jpg");

      // when
      ResourceLogResponse response1 =
          resourceCommandService.saveImageResourceCreated(request1).join();
      ResourceLogResponse response2 =
          resourceCommandService.saveImageResourceCreated(request2).join();

      // then
      assertEquals(1L, response1.id());
      assertEquals(2L, response2.id());
      assertEquals(2, resourceLogRepository.findAll().size());

      log.info("첫 번째 저장 = {}", response1);
      log.info("두 번째 저장 = {}", response2);
    }
  }

  @Nested
  @DisplayName("이미지 리소스 활성화 로그 저장 테스트")
  class ActivateImageResourceTest {

    @Test
    @DisplayName("이미지 리소스를 활성화할 때 ACTIVATED 이벤트로 저장된다")
    void test_1() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      resourceCommandService.saveImageResourceCreated(createRequest(1L, resourceKey)).join();

      // when
      CompletableFuture<Optional<ResourceLogResponse>> future =
          resourceCommandService.activateImageResource(resourceKey, 100L, "REVIEW");
      Optional<ResourceLogResponse> result = future.join();

      // then
      assertTrue(result.isPresent());
      assertEquals(ResourceEventType.ACTIVATED, result.get().eventType());
      assertEquals(100L, result.get().referenceId());
      assertEquals("REVIEW", result.get().referenceType());

      log.info("활성화 결과 = {}", result.get());
    }

    @Test
    @DisplayName("같은 리소스 키에 대해 CREATED, ACTIVATED 두 개의 로그가 저장된다")
    void test_2() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      resourceCommandService.saveImageResourceCreated(createRequest(1L, resourceKey)).join();

      // when
      resourceCommandService.activateImageResource(resourceKey, 100L, "REVIEW").join();

      // then
      List<ResourceLogResponse> logs = resourceCommandService.findByResourceKey(resourceKey);
      assertEquals(2, logs.size());

      log.info("저장된 로그 수 = {}", logs.size());
    }
  }

  @Nested
  @DisplayName("이미지 리소스 무효화 로그 저장 테스트")
  class InvalidateImageResourceTest {

    @Test
    @DisplayName("이미지 리소스를 무효화할 때 INVALIDATED 이벤트로 저장된다")
    void test_1() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      resourceCommandService.saveImageResourceCreated(createRequest(1L, resourceKey)).join();

      // when
      CompletableFuture<Optional<ResourceLogResponse>> future =
          resourceCommandService.invalidateImageResource(resourceKey);
      Optional<ResourceLogResponse> result = future.join();

      // then
      assertTrue(result.isPresent());
      assertEquals(ResourceEventType.INVALIDATED, result.get().eventType());

      log.info("무효화 결과 = {}", result.get());
    }
  }

  @Nested
  @DisplayName("리소스 로그 조회 테스트")
  class FindResourceLogTest {

    @Test
    @DisplayName("resourceKey로 최신 로그를 조회할 때 가장 최근 로그를 반환한다")
    void test_1() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      resourceCommandService.saveImageResourceCreated(createRequest(1L, resourceKey)).join();
      resourceCommandService.activateImageResource(resourceKey, 100L, "REVIEW").join();

      // when
      Optional<ResourceLogResponse> result =
          resourceCommandService.findLatestByResourceKey(resourceKey);

      // then
      assertTrue(result.isPresent());
      assertEquals(ResourceEventType.ACTIVATED, result.get().eventType());

      log.info("최신 로그 = {}", result.get());
    }

    @Test
    @DisplayName("존재하지 않는 resourceKey로 조회할 때 빈 결과를 반환한다")
    void test_2() {
      // given
      String resourceKey = "non-existent-key.jpg";

      // when
      Optional<ResourceLogResponse> result =
          resourceCommandService.findLatestByResourceKey(resourceKey);

      // then
      assertTrue(result.isEmpty());

      log.info("조회 결과 = {}", result);
    }
  }

  @Nested
  @DisplayName("이벤트 타입별 로그 조회 테스트")
  class FindByEventTypeTest {

    @Test
    @DisplayName("CREATED 이벤트와 날짜 기준으로 로그 목록을 조회할 때 조건에 맞는 목록을 반환한다")
    void test_1() {
      // given
      resourceCommandService
          .saveImageResourceCreated(createRequest(1L, "review/20251231/1-uuid1.jpg"))
          .join();
      resourceCommandService
          .saveImageResourceCreated(createRequest(2L, "review/20251231/2-uuid2.jpg"))
          .join();

      // createAt 설정 (과거 날짜로)
      resourceLogRepository
          .findById(1L)
          .ifPresent(
              log ->
                  ReflectionTestUtils.setField(log, "createAt", LocalDateTime.now().minusDays(7)));
      resourceLogRepository
          .findById(2L)
          .ifPresent(
              log ->
                  ReflectionTestUtils.setField(log, "createAt", LocalDateTime.now().minusDays(3)));

      // when
      List<ResourceLogItem> result =
          resourceCommandService.findByEventTypeAndCreateAtBefore(
              ResourceEventType.CREATED, LocalDateTime.now().minusDays(1));

      // then
      assertEquals(2, result.size());

      log.info("조회된 로그 수 = {}", result.size());
    }

    @Test
    @DisplayName("ACTIVATED 상태의 로그는 CREATED 조회 시 제외된다")
    void test_2() {
      // given
      String resourceKey = "review/20251231/1-uuid1.jpg";
      resourceCommandService.saveImageResourceCreated(createRequest(1L, resourceKey)).join();
      resourceLogRepository
          .findById(1L)
          .ifPresent(
              log ->
                  ReflectionTestUtils.setField(log, "createAt", LocalDateTime.now().minusDays(7)));
      resourceCommandService.activateImageResource(resourceKey, 100L, "REVIEW").join();

      // when
      List<ResourceLogItem> result =
          resourceCommandService.findByEventTypeAndCreateAtBefore(
              ResourceEventType.CREATED, LocalDateTime.now());

      // then
      assertEquals(1, result.size());
      assertEquals(ResourceEventType.CREATED, result.get(0).eventType());

      log.info("조회된 로그 수 = {}", result.size());
    }
  }
}
