package app.bottlenote.common.file.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.common.event.fixture.FakeApplicationEventPublisher;
import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.event.listener.ResourceEventListener;
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent;
import app.bottlenote.common.file.service.ResourceCommandService;
import app.bottlenote.common.file.upload.fixture.InMemoryResourceLogRepository;
import app.bottlenote.common.profanity.FakeProfanityClient;
import app.bottlenote.history.fixture.FakeHistoryEventPublisher;
import app.bottlenote.observability.service.LocalTracingService;
import app.bottlenote.review.dto.request.LocationInfoRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.fixture.InMemoryReviewRepository;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.support.business.constant.BusinessSupportType;
import app.bottlenote.support.business.dto.request.BusinessImageItem;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.fixture.InMemoryBusinessSupportRepository;
import app.bottlenote.support.business.service.BusinessSupportService;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.dto.request.HelpImageItem;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.fixture.InMemoryHelpRepository;
import app.bottlenote.support.help.service.HelpService;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] 이미지 리소스 활성화 이벤트 발행 테스트")
class ImageResourceActivatedEventPublishTest {

  @Nested
  @DisplayName("ReviewService 리뷰 이미지")
  class ReviewServiceTest {

    private ReviewService reviewService;
    private InMemoryReviewRepository reviewRepository;
    private FakeApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
      reviewRepository = new InMemoryReviewRepository();
      eventPublisher = new FakeApplicationEventPublisher();

      reviewService =
          new ReviewService(
              new FakeAlcoholFacade(),
              new FakeUserFacade(UserProfileItem.create(1L, "user1", "")),
              reviewRepository,
              new FakeHistoryEventPublisher(),
              new LocalTracingService(),
              eventPublisher);
    }

    @Test
    @DisplayName("이미지가 포함된 리뷰를 생성할 때 ImageResourceActivatedEvent가 발행된다")
    void test_create_review_with_images_publishes_event() {
      // given
      List<ReviewImageInfoRequest> images =
          List.of(
              new ReviewImageInfoRequest(1L, "https://cdn.bottlenote.com/review/img1.jpg"),
              new ReviewImageInfoRequest(2L, "https://cdn.bottlenote.com/review/img2.jpg"));
      ReviewCreateRequest request = createReviewRequest(images);

      // when
      ReviewCreateResponse response = reviewService.createReview(request, 1L);

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals(2, event.resourceKeys().size());
      assertEquals("review/img1.jpg", event.resourceKeys().get(0));
      assertEquals("review/img2.jpg", event.resourceKeys().get(1));
      assertEquals(response.getId(), event.referenceId());
      assertEquals("REVIEW", event.referenceType());
    }

    @Test
    @DisplayName("이미지 없이 리뷰를 생성할 때 이벤트가 발행되지 않는다")
    void test_create_review_without_images_does_not_publish_event() {
      // given
      ReviewCreateRequest request = createReviewRequest(List.of());

      // when
      reviewService.createReview(request, 1L);

      // then
      assertTrue(
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class).isEmpty());
    }

    @Test
    @DisplayName("이미지가 포함된 리뷰를 수정할 때 ImageResourceActivatedEvent가 발행된다")
    void test_modify_review_with_images_publishes_event() {
      // given
      ReviewCreateRequest createRequest = createReviewRequest(List.of());
      ReviewCreateResponse createResponse = reviewService.createReview(createRequest, 1L);
      eventPublisher.clear();

      List<ReviewImageInfoRequest> newImages =
          List.of(new ReviewImageInfoRequest(1L, "https://cdn.bottlenote.com/review/new-img.jpg"));
      ReviewModifyRequest modifyRequest =
          new ReviewModifyRequest(
              "수정된 내용", null, null, newImages, null, null, LocationInfoRequest.empty());

      // when
      reviewService.modifyReview(modifyRequest, createResponse.getId(), 1L);

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals("review/new-img.jpg", event.resourceKeys().get(0));
      assertEquals(createResponse.getId(), event.referenceId());
      assertEquals("REVIEW", event.referenceType());
    }

    private ReviewCreateRequest createReviewRequest(List<ReviewImageInfoRequest> images) {
      return new ReviewCreateRequest(
          1L, null, "테스트 리뷰 내용", null, null, LocationInfoRequest.empty(), images, List.of(), 4.5);
    }
  }

  @Nested
  @DisplayName("HelpService 문의 이미지")
  class HelpServiceTest {

    private HelpService helpService;
    private InMemoryHelpRepository helpRepository;
    private FakeApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
      helpRepository = new InMemoryHelpRepository();
      eventPublisher = new FakeApplicationEventPublisher();

      helpService =
          new HelpService(
              new FakeUserFacade(UserProfileItem.create(1L, "user1", "")),
              helpRepository,
              eventPublisher);
    }

    @Test
    @DisplayName("이미지가 포함된 문의를 등록할 때 ImageResourceActivatedEvent가 발행된다")
    void test_register_help_with_images_publishes_event() {
      // given
      List<HelpImageItem> images =
          List.of(
              new HelpImageItem(1L, "https://cdn.bottlenote.com/help/img1.jpg"),
              new HelpImageItem(2L, "https://cdn.bottlenote.com/help/img2.jpg"));
      HelpUpsertRequest request = new HelpUpsertRequest("제목", "내용", HelpType.USER, images);

      // when
      HelpResultResponse response = helpService.registerHelp(request, 1L);

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals(2, event.resourceKeys().size());
      assertEquals("help/img1.jpg", event.resourceKeys().get(0));
      assertEquals("help/img2.jpg", event.resourceKeys().get(1));
      assertEquals(response.helpId(), event.referenceId());
      assertEquals("HELP", event.referenceType());
    }

    @Test
    @DisplayName("이미지 없이 문의를 등록할 때 이벤트가 발행되지 않는다")
    void test_register_help_without_images_does_not_publish_event() {
      // given
      HelpUpsertRequest request = new HelpUpsertRequest("제목", "내용", HelpType.USER, List.of());

      // when
      helpService.registerHelp(request, 1L);

      // then
      assertTrue(
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class).isEmpty());
    }

    @Test
    @DisplayName("이미지가 포함된 문의를 수정할 때 ImageResourceActivatedEvent가 발행된다")
    void test_modify_help_with_images_publishes_event() {
      // given
      HelpUpsertRequest createRequest = new HelpUpsertRequest("제목", "내용", HelpType.USER, List.of());
      HelpResultResponse createResponse = helpService.registerHelp(createRequest, 1L);
      eventPublisher.clear();

      List<HelpImageItem> newImages =
          List.of(new HelpImageItem(1L, "https://cdn.bottlenote.com/help/new-img.jpg"));
      HelpUpsertRequest modifyRequest =
          new HelpUpsertRequest("수정 제목", "수정 내용", HelpType.USER, newImages);

      // when
      helpService.modifyHelp(modifyRequest, 1L, createResponse.helpId());

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals("help/new-img.jpg", event.resourceKeys().get(0));
      assertEquals(createResponse.helpId(), event.referenceId());
      assertEquals("HELP", event.referenceType());
    }
  }

  @Nested
  @DisplayName("BusinessSupportService 사업 제휴 이미지")
  class BusinessSupportServiceTest {

    private BusinessSupportService businessSupportService;
    private InMemoryBusinessSupportRepository businessSupportRepository;
    private FakeApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
      businessSupportRepository = new InMemoryBusinessSupportRepository();
      eventPublisher = new FakeApplicationEventPublisher();

      businessSupportService =
          new BusinessSupportService(
              businessSupportRepository,
              new FakeUserFacade(UserProfileItem.create(1L, "user1", "")),
              new FakeProfanityClient(),
              eventPublisher);
    }

    @Test
    @DisplayName("이미지가 포함된 사업 제휴를 등록할 때 ImageResourceActivatedEvent가 발행된다")
    void test_register_business_with_images_publishes_event() {
      // given
      List<BusinessImageItem> images =
          List.of(
              BusinessImageItem.create(1L, "https://cdn.bottlenote.com/business/img1.jpg"),
              BusinessImageItem.create(2L, "https://cdn.bottlenote.com/business/img2.jpg"));
      BusinessSupportUpsertRequest request =
          new BusinessSupportUpsertRequest(
              "제목", "내용", "test@test.com", BusinessSupportType.EVENT, images);

      // when
      BusinessSupportResultResponse response = businessSupportService.register(request, 1L);

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals(2, event.resourceKeys().size());
      assertEquals("business/img1.jpg", event.resourceKeys().get(0));
      assertEquals("business/img2.jpg", event.resourceKeys().get(1));
      assertEquals(response.id(), event.referenceId());
      assertEquals("BUSINESS", event.referenceType());
    }

    @Test
    @DisplayName("이미지 없이 사업 제휴를 등록할 때 이벤트가 발행되지 않는다")
    void test_register_business_without_images_does_not_publish_event() {
      // given
      BusinessSupportUpsertRequest request =
          new BusinessSupportUpsertRequest(
              "제목", "내용", "test@test.com", BusinessSupportType.EVENT, List.of());

      // when
      businessSupportService.register(request, 1L);

      // then
      assertTrue(
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class).isEmpty());
    }

    @Test
    @DisplayName("이미지가 포함된 사업 제휴를 수정할 때 ImageResourceActivatedEvent가 발행된다")
    void test_modify_business_with_images_publishes_event() {
      // given
      BusinessSupportUpsertRequest createRequest =
          new BusinessSupportUpsertRequest(
              "제목", "내용", "test@test.com", BusinessSupportType.EVENT, List.of());
      BusinessSupportResultResponse createResponse =
          businessSupportService.register(createRequest, 1L);
      eventPublisher.clear();

      List<BusinessImageItem> newImages =
          List.of(BusinessImageItem.create(1L, "https://cdn.bottlenote.com/business/new-img.jpg"));
      BusinessSupportUpsertRequest modifyRequest =
          new BusinessSupportUpsertRequest(
              "수정 제목", "수정 내용", "test@test.com", BusinessSupportType.EVENT, newImages);

      // when
      businessSupportService.modify(createResponse.id(), modifyRequest, 1L);

      // then
      List<ImageResourceActivatedEvent> events =
          eventPublisher.getPublishedEventsOfType(ImageResourceActivatedEvent.class);
      assertEquals(1, events.size());

      ImageResourceActivatedEvent event = events.get(0);
      assertEquals("business/new-img.jpg", event.resourceKeys().get(0));
      assertEquals(createResponse.id(), event.referenceId());
      assertEquals("BUSINESS", event.referenceType());
    }
  }

  @Nested
  @DisplayName("이벤트 발행 후 ResourceLog 상태 변경 검증")
  class ResourceLogActivationTest {

    private InMemoryResourceLogRepository resourceLogRepository;
    private ResourceCommandService resourceCommandService;
    private ResourceEventListener resourceEventListener;

    @BeforeEach
    void setUp() {
      resourceLogRepository = new InMemoryResourceLogRepository();
      resourceCommandService = new ResourceCommandService(resourceLogRepository);
      resourceEventListener = new ResourceEventListener(resourceCommandService);
    }

    @Test
    @DisplayName("이벤트를 수신할 때 ResourceLog에 ACTIVATED 상태로 로그가 저장된다")
    void test_event_listener_creates_activated_log() {
      // given
      String resourceKey = "review/test-image.jpg";
      Long referenceId = 100L;
      String referenceType = "REVIEW";

      resourceCommandService
          .saveImageResourceCreated(
              new ResourceLogRequest(
                  1L,
                  resourceKey,
                  "https://cdn.bottlenote.com/" + resourceKey,
                  "review",
                  "test-bucket"))
          .join();

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, referenceId, referenceType);

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs = resourceLogRepository.findByResourceKey(resourceKey);
      assertFalse(logs.isEmpty());

      ResourceLog activatedLog =
          logs.stream()
              .filter(log -> log.getEventType() == ResourceEventType.ACTIVATED)
              .findFirst()
              .orElse(null);

      assertNotNull(activatedLog);
      assertEquals(ResourceEventType.ACTIVATED, activatedLog.getEventType());
      assertEquals(referenceId, activatedLog.getReferenceId());
      assertEquals(referenceType, activatedLog.getReferenceType());
      assertEquals(resourceKey, activatedLog.getResourceKey());
    }

    @Test
    @DisplayName("여러 이미지 리소스 활성화 이벤트를 수신할 때 각각 ACTIVATED 로그가 저장된다")
    void test_multiple_resources_create_multiple_activated_logs() {
      // given
      String resourceKey1 = "help/image1.jpg";
      String resourceKey2 = "help/image2.jpg";
      Long referenceId = 200L;
      String referenceType = "HELP";

      resourceCommandService
          .saveImageResourceCreated(
              new ResourceLogRequest(
                  1L,
                  resourceKey1,
                  "https://cdn.bottlenote.com/" + resourceKey1,
                  "help",
                  "test-bucket"))
          .join();
      resourceCommandService
          .saveImageResourceCreated(
              new ResourceLogRequest(
                  1L,
                  resourceKey2,
                  "https://cdn.bottlenote.com/" + resourceKey2,
                  "help",
                  "test-bucket"))
          .join();

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(
              List.of(resourceKey1, resourceKey2), referenceId, referenceType);

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs1 = resourceLogRepository.findByResourceKey(resourceKey1);
      List<ResourceLog> logs2 = resourceLogRepository.findByResourceKey(resourceKey2);

      ResourceLog activated1 =
          logs1.stream()
              .filter(log -> log.getEventType() == ResourceEventType.ACTIVATED)
              .findFirst()
              .orElse(null);
      ResourceLog activated2 =
          logs2.stream()
              .filter(log -> log.getEventType() == ResourceEventType.ACTIVATED)
              .findFirst()
              .orElse(null);

      assertNotNull(activated1);
      assertNotNull(activated2);
      assertEquals(referenceId, activated1.getReferenceId());
      assertEquals(referenceId, activated2.getReferenceId());
      assertEquals(referenceType, activated1.getReferenceType());
      assertEquals(referenceType, activated2.getReferenceType());
    }

    @Test
    @DisplayName("CREATED 로그가 있는 리소스를 활성화할 때 CREATED와 ACTIVATED 로그가 모두 존재한다")
    void test_resource_log_sequence_created_to_activated() {
      // given
      String resourceKey = "business/document.pdf";
      Long userId = 1L;
      Long referenceId = 300L;
      String referenceType = "BUSINESS";

      resourceCommandService
          .saveImageResourceCreated(
              new ResourceLogRequest(
                  userId,
                  resourceKey,
                  "https://cdn.bottlenote.com/" + resourceKey,
                  "business",
                  "test-bucket"))
          .join();

      ImageResourceActivatedEvent event =
          ImageResourceActivatedEvent.of(resourceKey, referenceId, referenceType);

      // when
      resourceEventListener.handleImageResourceActivated(event);

      // then
      List<ResourceLog> logs = resourceLogRepository.findByResourceKey(resourceKey);
      assertEquals(2, logs.size());

      boolean hasCreated =
          logs.stream().anyMatch(log -> log.getEventType() == ResourceEventType.CREATED);
      boolean hasActivated =
          logs.stream().anyMatch(log -> log.getEventType() == ResourceEventType.ACTIVATED);

      assertTrue(hasCreated);
      assertTrue(hasActivated);
    }
  }
}
