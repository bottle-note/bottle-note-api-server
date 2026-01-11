package app.bottlenote.common.file.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.event.listener.ResourceEventListener;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [event] ResourceEventListener")
class ResourceEventListenerTest {

  private ResourceEventListener resourceEventListener;
  private ResourceCommandService resourceCommandService;
  private InMemoryResourceLogRepository resourceLogRepository;

  @BeforeEach
  void setUp() {
    resourceLogRepository = new InMemoryResourceLogRepository();
    resourceCommandService = new ResourceCommandService(resourceLogRepository);
    resourceEventListener = new ResourceEventListener(resourceCommandService);
  }

  private void createResourceLog(String resourceKey, Long userId) {
    ResourceLogRequest request =
        ResourceLogRequest.builder()
            .userId(userId)
            .resourceKey(resourceKey)
            .viewUrl("https://cdn.example.com/" + resourceKey)
            .rootPath("review")
            .bucketName("test-bucket")
            .build();
    resourceCommandService.saveImageResourceCreated(request).join();
  }

  @Nested
  @DisplayName("이미지 리소스 활성화 이벤트 처리 테스트")
  class HandleImageResourceActivatedTest {

    @Test
    @DisplayName("단일 리소스 키에 대해 ACTIVATED 로그가 저장된다")
    void test_single_resource_key() {
      // given
      String resourceKey = "review/20251231/1-uuid.jpg";
      createResourceLog(resourceKey, 1L);

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, 100L, "REVIEW");

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs =
          resourceLogRepository.findByReferenceIdAndReferenceType(100L, "REVIEW");
      assertEquals(1, logs.size());
      assertEquals(ResourceEventType.ACTIVATED, logs.get(0).getEventType());
      assertEquals(100L, logs.get(0).getReferenceId());
      assertEquals("REVIEW", logs.get(0).getReferenceType());
    }

    @Test
    @DisplayName("여러 리소스 키에 대해 각각 ACTIVATED 로그가 저장된다")
    void test_multiple_resource_keys() {
      // given
      String resourceKey1 = "review/20251231/1-uuid1.jpg";
      String resourceKey2 = "review/20251231/2-uuid2.jpg";
      String resourceKey3 = "review/20251231/3-uuid3.jpg";
      createResourceLog(resourceKey1, 1L);
      createResourceLog(resourceKey2, 1L);
      createResourceLog(resourceKey3, 1L);

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(
              List.of(resourceKey1, resourceKey2, resourceKey3), 200L, "REVIEW");

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs =
          resourceLogRepository.findByReferenceIdAndReferenceType(200L, "REVIEW");
      assertEquals(3, logs.size());
      logs.forEach(
          log -> {
            assertEquals(ResourceEventType.ACTIVATED, log.getEventType());
            assertEquals(200L, log.getReferenceId());
            assertEquals("REVIEW", log.getReferenceType());
          });
    }

    @Test
    @DisplayName("PROFILE 타입의 리소스에 대해 ACTIVATED 로그가 저장된다")
    void test_profile_reference_type() {
      // given
      String resourceKey = "profile/20251231/1-uuid.jpg";
      createResourceLog(resourceKey, 1L);

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, 1L, "PROFILE");

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs =
          resourceLogRepository.findByReferenceIdAndReferenceType(1L, "PROFILE");
      assertEquals(1, logs.size());
      assertEquals("PROFILE", logs.get(0).getReferenceType());
    }

    @Test
    @DisplayName("HELP 타입의 리소스에 대해 ACTIVATED 로그가 저장된다")
    void test_help_reference_type() {
      // given
      String resourceKey = "help/20251231/1-uuid.jpg";
      createResourceLog(resourceKey, 1L);

      ImageResourceActivatedEvent event = ImageResourceActivatedEvent.of(resourceKey, 50L, "HELP");

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs = resourceLogRepository.findByReferenceIdAndReferenceType(50L, "HELP");
      assertEquals(1, logs.size());
      assertEquals("HELP", logs.get(0).getReferenceType());
    }

    @Test
    @DisplayName("BUSINESS 타입의 리소스에 대해 ACTIVATED 로그가 저장된다")
    void test_business_reference_type() {
      // given
      String resourceKey = "business/20251231/1-uuid.jpg";
      createResourceLog(resourceKey, 1L);

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, 30L, "BUSINESS");

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs =
          resourceLogRepository.findByReferenceIdAndReferenceType(30L, "BUSINESS");
      assertEquals(1, logs.size());
      assertEquals("BUSINESS", logs.get(0).getReferenceType());
    }
  }
}
