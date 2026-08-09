package app.bottlenote.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationActionFallbackType;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.request.NotificationPageableRequest;
import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.exception.NotificationExceptionCode;
import app.bottlenote.notification.fixture.InMemoryNotificationRepository;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("UserNotificationService 단위 테스트")
class UserNotificationServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;

  private FakeUserFacade userFacade;
  private InMemoryNotificationRepository notificationRepository;
  private UserNotificationService service;

  @BeforeEach
  void setUp() {
    userFacade = new FakeUserFacade();
    notificationRepository = new InMemoryNotificationRepository();
    service =
        new UserNotificationService(userFacade, notificationRepository);
  }

  @Nested
  @DisplayName("알림을 생성할 때")
  class SendNotification {

    @Test
    @DisplayName("대상 사용자가 있으면 알림을 저장한다")
    void sendNotification_whenUserExists_savesNotification() {
      seedUser(USER_ID);

      service.sendNotification(
          NotificationMessage.create(
              USER_ID,
              NotificationType.USER,
              NotificationCategory.REVIEW,
              "새 댓글",
              "리뷰에 댓글이 달렸습니다."));

      assertThat(notificationRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(
              notification -> {
                assertThat(notification.getUserId()).isEqualTo(USER_ID);
                assertThat(notification.getTitle()).isEqualTo("새 댓글");
                assertThat(notification.getContent()).isEqualTo("리뷰에 댓글이 달렸습니다.");
                assertThat(notification.getType()).isEqualTo(NotificationType.USER);
                assertThat(notification.getCategory()).isEqualTo(NotificationCategory.REVIEW);
                assertThat(notification.getIsRead()).isFalse();
              });
    }

    @Test
    @DisplayName("대상 사용자가 없으면 예외를 던진다")
    void sendNotification_whenUserMissing_throwsException() {
      NotificationMessage message =
          NotificationMessage.create(
              USER_ID, NotificationType.USER, NotificationCategory.REVIEW, "제목", "내용");

      assertThatThrownBy(() -> service.sendNotification(message))
          .isInstanceOf(UserException.class)
          .extracting(ex -> ((UserException) ex).getExceptionCode())
          .isEqualTo(UserExceptionCode.NOTIFICATION_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("동일 댓글 알림을 순차 전송하면 한 건만 저장한다")
    void sendNotification_whenReviewReplyDuplicated_savesOnce() {
      seedUser(USER_ID);
      NotificationMessage message =
          NotificationMessage.reviewReply(USER_ID, 10L, 20L, "새 댓글", "내용");

      service.sendNotification(message);
      service.sendNotification(message);

      assertThat(notificationRepository.findAll())
          .singleElement()
          .satisfies(
              notification -> {
                assertThat(notification.getSourceType())
                    .isEqualTo(NotificationSourceType.REVIEW_REPLY.name());
                assertThat(notification.getSourceId()).isEqualTo(20L);
                assertThat(notification.getActionType()).isEqualTo("OPEN_REVIEW");
                assertThat(notification.getActionTargetId()).isEqualTo(10L);
                assertThat(notification.getActionPayload().path("replyId").longValue())
                    .isEqualTo(20L);
                assertThat(notification.getActionVersion()).isEqualTo((short) 1);
              });
    }

    @Test
    @DisplayName("원본이 없는 레거시 메시지는 기존처럼 매번 저장한다")
    void sendNotification_whenLegacyMessageDuplicated_savesEachTime() {
      seedUser(USER_ID);
      NotificationMessage message =
          NotificationMessage.create(
              USER_ID, NotificationType.USER, NotificationCategory.REVIEW, "제목", "내용");

      service.sendNotification(message);
      service.sendNotification(message);

      assertThat(notificationRepository.findAll()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("알림함을 조회할 때")
  class InboxQuery {

    @Test
    @DisplayName("사용자별 알림을 최신 id 순으로 반환한다")
    void getNotifications_whenMultipleExist_returnsOwnOrderedByIdDesc() {
      Notification older = seedNotification(USER_ID, "old");
      Notification newer = seedNotification(USER_ID, "new");
      seedNotification(OTHER_USER_ID, "other");

      PageResponse<NotificationListResponse> result =
          service.getNotifications(USER_ID, NotificationPageableRequest.builder().build());

      assertThat(result.content().totalCount()).isEqualTo(2);
      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(newer.getId(), older.getId());
      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::title)
          .containsExactly("new", "old");
      assertThat(result.cursorPageable().getHasNext()).isFalse();
      assertThat(result.cursorPageable().getCurrentCursor()).isZero();
      assertThat(result.cursorPageable().getCursor()).isEqualTo(older.getId());
      assertThat(result.cursorPageable().getPageSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("id-desc keyset으로 다음 페이지를 조회하고 nextCursor는 마지막 item id다")
    void getNotifications_whenKeysetCursor_returnsNextPageById() {
      Notification n1 = seedNotification(USER_ID, "n1");
      Notification n2 = seedNotification(USER_ID, "n2");
      Notification n3 = seedNotification(USER_ID, "n3");

      PageResponse<NotificationListResponse> firstPage =
          service.getNotifications(
              USER_ID, NotificationPageableRequest.builder().cursor(0L).pageSize(2L).build());

      assertThat(firstPage.content().totalCount()).isEqualTo(3);
      assertThat(firstPage.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(n3.getId(), n2.getId());
      assertThat(firstPage.cursorPageable().getHasNext()).isTrue();
      assertThat(firstPage.cursorPageable().getCurrentCursor()).isZero();
      // nextCursor = 마지막 반환 item id
      assertThat(firstPage.cursorPageable().getCursor()).isEqualTo(n2.getId());

      PageResponse<NotificationListResponse> secondPage =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder().cursor(n2.getId()).pageSize(2L).build());

      assertThat(secondPage.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(n1.getId());
      assertThat(secondPage.cursorPageable().getHasNext()).isFalse();
      assertThat(secondPage.cursorPageable().getCurrentCursor()).isEqualTo(n2.getId());
      assertThat(secondPage.cursorPageable().getCursor()).isEqualTo(n1.getId());
    }

    @Test
    @DisplayName("알림 타입으로 목록과 totalCount를 필터링한다")
    void getNotifications_whenTypeFilterExists_returnsMatchingType() {
      seedNotification(USER_ID, "user");
      notificationRepository.save(
          Notification.builder()
              .userId(USER_ID)
              .title("system")
              .content("system-content")
              .type(NotificationType.SYSTEM)
              .category(NotificationCategory.REVIEW)
              .build());

      PageResponse<NotificationListResponse> result =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .types(List.of(NotificationType.SYSTEM))
                  .build());

      assertThat(result.content().totalCount()).isOne();
      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::type)
          .containsExactly(NotificationType.SYSTEM);
    }

    @Test
    @DisplayName("알림 카테고리로 목록을 필터링한다")
    void getNotifications_whenCategoryFilterExists_returnsMatchingCategory() {
      seedNotification(USER_ID, "review");
      notificationRepository.save(
          Notification.builder()
              .userId(USER_ID)
              .title("notice")
              .content("notice-content")
              .type(NotificationType.USER)
              .category(NotificationCategory.NOTICE)
              .build());

      PageResponse<NotificationListResponse> result =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .categories(List.of(NotificationCategory.NOTICE))
                  .build());

      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::category)
          .containsExactly(NotificationCategory.NOTICE);
    }

    @Test
    @DisplayName("READ UNREAD ALL을 isRead 기준으로 필터링한다")
    void getNotifications_whenReadStatusChanges_filtersByIsRead() {
      Notification unread = seedNotification(USER_ID, "unread");
      Notification read = seedNotification(USER_ID, "read");
      read.markAsRead(LocalDateTime.of(2026, 8, 10, 10, 0));

      PageResponse<NotificationListResponse> readResult =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .readStatus(NotificationReadStatus.READ)
                  .build());
      PageResponse<NotificationListResponse> unreadResult =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .readStatus(NotificationReadStatus.UNREAD)
                  .build());
      PageResponse<NotificationListResponse> allResult =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .readStatus(NotificationReadStatus.ALL)
                  .build());

      assertThat(readResult.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(read.getId());
      assertThat(unreadResult.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(unread.getId());
      assertThat(allResult.content().totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("생성 시각은 from 포함 to 제외 반개구간으로 필터링한다")
    void getNotifications_whenCreatedRangeExists_usesHalfOpenIntervalInKst() {
      Notification before = seedNotification(USER_ID, "before");
      Notification from = seedNotification(USER_ID, "from");
      Notification inside = seedNotification(USER_ID, "inside");
      Notification to = seedNotification(USER_ID, "to");
      ReflectionTestUtils.setField(before, "createAt", LocalDateTime.of(2026, 8, 10, 8, 59));
      ReflectionTestUtils.setField(from, "createAt", LocalDateTime.of(2026, 8, 10, 9, 0));
      ReflectionTestUtils.setField(inside, "createAt", LocalDateTime.of(2026, 8, 10, 9, 30));
      ReflectionTestUtils.setField(to, "createAt", LocalDateTime.of(2026, 8, 10, 10, 0));

      PageResponse<NotificationListResponse> result =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .createdFrom(OffsetDateTime.parse("2026-08-10T00:00:00Z"))
                  .createdTo(OffsetDateTime.parse("2026-08-10T01:00:00Z"))
                  .build());

      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactly(inside.getId(), from.getId());
      assertThat(result.content().totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("결합 필터의 다중 cursor 페이지는 중복과 누락 없이 id 내림차순이다")
    void getNotifications_whenCombinedFilterIsPaged_hasNoDuplicatesOrMissingItems() {
      Notification n1 = seedNotification(USER_ID, "n1");
      Notification n2 = seedNotification(USER_ID, "n2");
      Notification n3 = seedNotification(USER_ID, "n3");
      seedNotification(OTHER_USER_ID, "other");
      seedNotification(USER_ID, "excluded").markAsRead();
      NotificationPageableRequest firstRequest =
          NotificationPageableRequest.builder()
              .types(List.of(NotificationType.USER))
              .categories(List.of(NotificationCategory.REVIEW))
              .readStatus(NotificationReadStatus.UNREAD)
              .pageSize(2L)
              .build();

      PageResponse<NotificationListResponse> first =
          service.getNotifications(USER_ID, firstRequest);
      PageResponse<NotificationListResponse> second =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder()
                  .cursor(first.cursorPageable().getCursor())
                  .pageSize(2L)
                  .types(firstRequest.types())
                  .categories(firstRequest.categories())
                  .readStatus(firstRequest.readStatus())
                  .build());

      assertThat(first.content().totalCount()).isEqualTo(3);
      assertThat(second.content().totalCount()).isEqualTo(3);
      assertThat(
              java.util.stream.Stream.concat(
                      first.content().items().stream(), second.content().items().stream())
                  .map(NotificationListResponse.Item::id)
                  .toList())
          .containsExactly(n3.getId(), n2.getId(), n1.getId());
    }

    @Test
    @DisplayName("빈 타입과 카테고리 목록은 전체를 조회한다")
    void getNotifications_whenCollectionFiltersAreEmpty_returnsAll() {
      seedNotification(USER_ID, "review");
      notificationRepository.save(
          Notification.builder()
              .userId(USER_ID)
              .title("notice")
              .content("notice-content")
              .type(NotificationType.SYSTEM)
              .category(NotificationCategory.NOTICE)
              .build());

      PageResponse<NotificationListResponse> result =
          service.getNotifications(
              USER_ID,
              NotificationPageableRequest.builder().types(List.of()).categories(List.of()).build());

      assertThat(result.content().totalCount()).isEqualTo(2);
      assertThat(result.content().items()).hasSize(2);
    }

    @Test
    @DisplayName("유효한 OPEN_REVIEW Action을 typed 계약으로 반환한다")
    void getNotifications_whenOpenReviewActionIsValid_returnsTypedAction() {
      Notification notification =
          seedNotification(USER_ID, "action", NotificationAction.openReview(10L, 20L));
      LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 10, 12, 0);
      ReflectionTestUtils.setField(notification, "createAt", occurredAt);
      notification.markAsRead(occurredAt);

      NotificationListResponse.Item item =
          service
              .getNotifications(USER_ID, NotificationPageableRequest.builder().build())
              .content()
              .items()
              .getFirst();

      assertThat(item.id()).isEqualTo(notification.getId());
      assertThat(item.action().type()).isEqualTo(NotificationActionType.OPEN_REVIEW);
      assertThat(item.action().targetId()).isEqualTo(10L);
      assertThat(item.action().payload().replyId()).isEqualTo(20L);
      assertThat(item.action().version()).isEqualTo(1);
      assertThat(item.action().fallbackType())
          .isEqualTo(NotificationActionFallbackType.OPEN_NOTIFICATION_CENTER);
      assertThat(item.createAt().toString()).isEqualTo("2026-08-10T12:00+09:00");
      assertThat(item.readAt().toString()).isEqualTo("2026-08-10T12:00+09:00");
    }

    @Test
    @DisplayName("미지원 또는 불완전 Action은 항목별 null로 강등한다")
    void getNotifications_whenActionsAreInvalid_downgradesEachActionToNull() {
      Notification unsupportedType =
          seedRawAction("OPEN_URL", 10L, Map.of("replyId", 20L), (short) 1);
      Notification unsupportedVersion =
          seedRawAction("OPEN_REVIEW", 10L, Map.of("replyId", 20L), (short) 2);
      Notification extraKey =
          seedRawAction(
              "OPEN_REVIEW", 10L, Map.of("replyId", 20L, "url", "invalid"), (short) 1);
      Notification wrongType =
          seedRawAction("OPEN_REVIEW", 10L, Map.of("replyId", "20"), (short) 1);
      Notification nonPositiveTarget =
          seedRawAction("OPEN_REVIEW", 0L, Map.of("replyId", 20L), (short) 1);
      Notification oversized =
          seedRawAction(
              "OPEN_REVIEW",
              10L,
              Map.of("replyId", new BigInteger("9".repeat(1025))),
              (short) 1);
      Notification scalarPayload = seedRawAction("OPEN_REVIEW", 10L, "20", (short) 1);
      Notification listPayload = seedRawAction("OPEN_REVIEW", 10L, List.of(20L), (short) 1);
      Notification missingPayload = seedRawAction("OPEN_REVIEW", 10L, null, (short) 1);
      Notification missingReplyId = seedRawAction("OPEN_REVIEW", 10L, Map.of(), (short) 1);
      Notification legacy = seedNotification(USER_ID, "legacy");
      Notification valid =
          seedNotification(USER_ID, "valid", NotificationAction.openReview(10L, 20L));

      PageResponse<NotificationListResponse> result =
          service.getNotifications(
              USER_ID, NotificationPageableRequest.builder().pageSize(20L).build());

      assertThat(result.content().items()).hasSize(12);
      assertThat(result.content().items())
          .extracting(NotificationListResponse.Item::id)
          .containsExactlyInAnyOrder(
              unsupportedType.getId(),
              unsupportedVersion.getId(),
              extraKey.getId(),
              wrongType.getId(),
              nonPositiveTarget.getId(),
              oversized.getId(),
              scalarPayload.getId(),
              listPayload.getId(),
              missingPayload.getId(),
              missingReplyId.getId(),
              legacy.getId(),
              valid.getId());
      assertThat(result.content().items())
          .filteredOn(item -> !item.id().equals(valid.getId()))
          .extracting(NotificationListResponse.Item::action)
          .containsOnlyNulls();
      assertThat(result.content().items())
          .filteredOn(item -> item.id().equals(valid.getId()))
          .extracting(NotificationListResponse.Item::action)
          .doesNotContainNull();
    }

    @Test
    @DisplayName("OPEN_REVIEW 저장 계약은 양수 식별자와 정확한 payload key를 강제한다")
    void openReview_whenCreatingAction_validatesStorageContract() {
      NotificationAction action = NotificationAction.openReview(10L, 20L);

      assertThat(action.payload().path("replyId").longValue()).isEqualTo(20L);
      assertThatThrownBy(() -> NotificationAction.openReview(0L, 20L))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> NotificationAction.openReview(10L, 0L))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("미읽음 개수를 사용자 기준으로 센다")
    void countUnread_whenMixedReadState_countsOnlyUnreadOfUser() {
      Notification unread = seedNotification(USER_ID, "unread-1");
      Notification read = seedNotification(USER_ID, "read");
      service.markAsRead(USER_ID, read.getId());
      seedNotification(USER_ID, "unread-2");
      seedNotification(OTHER_USER_ID, "other-unread");

      long count = service.countUnread(USER_ID);

      assertThat(count).isEqualTo(2L);
      assertThat(unread.getIsRead()).isFalse();
    }
  }

  @Nested
  @DisplayName("알림을 읽음 처리할 때")
  class MarkRead {

    @Test
    @DisplayName("본인 미읽음 알림은 최초 읽음 시각을 기록한다")
    void markAsRead_whenUnreadNotification_recordsFirstReadAt() {
      Notification notification =
          seedNotification(USER_ID, "title", NotificationStatus.SENT, false, null);

      NotificationMarkReadResult result = service.markAsRead(USER_ID, notification.getId());

      Notification saved = notificationRepository.findById(notification.getId()).orElseThrow();
      assertThat(result.notificationId()).isEqualTo(notification.getId());
      assertThat(result.isRead()).isTrue();
      assertThat(result.readAt()).isEqualTo(saved.getReadAt());
      assertThat(result.changed()).isTrue();
      assertThat(result.unreadCount()).isZero();
      assertThat(saved.getIsRead()).isTrue();
      assertThat(saved.getReadAt()).isNotNull();
      assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("이미 읽은 알림을 다시 읽어도 최초 읽음 시각을 유지한다")
    void markAsRead_whenCalledAgain_preservesFirstReadAt() {
      Notification notification = seedNotification(USER_ID, "title");
      NotificationMarkReadResult first = service.markAsRead(USER_ID, notification.getId());
      LocalDateTime firstReadAt = notification.getReadAt();

      NotificationMarkReadResult second = service.markAsRead(USER_ID, notification.getId());

      assertThat(first.changed()).isTrue();
      assertThat(second.changed()).isFalse();
      assertThat(second.readAt()).isEqualTo(first.readAt());
      assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    @DisplayName("읽음 처리할 때 전달 상태를 변경하지 않는다")
    void markAsRead_whenDeliveryStatusExists_preservesStatus() {
      Notification pending =
          seedNotification(USER_ID, "pending", NotificationStatus.PENDING, false, null);
      Notification sent = seedNotification(USER_ID, "sent", NotificationStatus.SENT, false, null);
      Notification failed =
          seedNotification(USER_ID, "failed", NotificationStatus.FAILED, false, null);

      service.markAsRead(USER_ID, pending.getId());
      service.markAsRead(USER_ID, sent.getId());
      service.markAsRead(USER_ID, failed.getId());

      assertThat(pending.getStatus()).isEqualTo(NotificationStatus.PENDING);
      assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
      assertThat(failed.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("기존 읽음 시각이 없는 알림은 null을 유지한다")
    void markAsRead_whenLegacyReadAtIsNull_preservesNull() {
      Notification notification =
          seedNotification(USER_ID, "legacy", NotificationStatus.READ, true, null);

      NotificationMarkReadResult result = service.markAsRead(USER_ID, notification.getId());

      assertThat(result.isRead()).isTrue();
      assertThat(result.readAt()).isNull();
      assertThat(result.changed()).isFalse();
      assertThat(notification.getIsRead()).isTrue();
      assertThat(notification.getReadAt()).isNull();
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("타 사용자 알림은 찾을 수 없음으로 처리한다")
    void markAsRead_whenOtherUsersNotification_throwsNotFound() {
      Notification other = seedNotification(OTHER_USER_ID, "other");

      assertThatThrownBy(() -> service.markAsRead(USER_ID, other.getId()))
          .isInstanceOf(NotificationException.class)
          .extracting(ex -> ((NotificationException) ex).getExceptionCode())
          .isEqualTo(NotificationExceptionCode.NOTIFICATION_NOT_FOUND);
      assertThat(other.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 알림이면 예외를 던진다")
    void markAsRead_whenMissing_throwsNotFound() {
      assertThatThrownBy(() -> service.markAsRead(USER_ID, 999L))
          .isInstanceOf(NotificationException.class)
          .extracting(ex -> ((NotificationException) ex).getExceptionCode())
          .isEqualTo(NotificationExceptionCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 읽음은 본인 미읽음에만 시각을 기록하고 전달 상태를 유지한다")
    void markAllAsRead_whenUnreadExist_recordsOwnReadAtAndPreservesStatus() {
      Notification first =
          seedNotification(USER_ID, "a", NotificationStatus.SENT, false, null);
      Notification second =
          seedNotification(USER_ID, "b", NotificationStatus.FAILED, false, null);
      Notification other = seedNotification(OTHER_USER_ID, "other");

      int updated = service.markAllAsRead(USER_ID);

      assertThat(updated).isEqualTo(2);
      assertThat(first.getIsRead()).isTrue();
      assertThat(second.getIsRead()).isTrue();
      assertThat(first.getReadAt()).isNotNull();
      assertThat(second.getReadAt()).isEqualTo(first.getReadAt());
      assertThat(first.getStatus()).isEqualTo(NotificationStatus.SENT);
      assertThat(second.getStatus()).isEqualTo(NotificationStatus.FAILED);
      assertThat(other.getIsRead()).isFalse();
      assertThat(other.getReadAt()).isNull();
      assertThat(service.countUnread(USER_ID)).isZero();
      assertThat(service.countUnread(OTHER_USER_ID)).isEqualTo(1L);
    }

    @Test
    @DisplayName("전체 읽음은 기존 최초 읽음 시각을 변경하지 않는다")
    void markAllAsRead_whenAlreadyReadExists_preservesExistingReadAt() {
      LocalDateTime existingReadAt = LocalDateTime.of(2026, 8, 1, 12, 0);
      Notification alreadyRead =
          seedNotification(USER_ID, "read", NotificationStatus.SENT, true, existingReadAt);
      Notification unread = seedNotification(USER_ID, "unread");

      int updated = service.markAllAsRead(USER_ID);

      assertThat(updated).isEqualTo(1);
      assertThat(alreadyRead.getReadAt()).isEqualTo(existingReadAt);
      assertThat(unread.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 모두 읽은 경우 0건을 반환한다")
    void markAllAsRead_whenNoneUnread_returnsZero() {
      Notification notification = seedNotification(USER_ID, "a");
      service.markAsRead(USER_ID, notification.getId());

      int updated = service.markAllAsRead(USER_ID);

      assertThat(updated).isZero();
    }
  }

  private void seedUser(Long userId) {
    userFacade.addUser(UserProfileItem.create(userId, "user" + userId, ""));
  }

  private Notification seedNotification(Long userId, String title) {
    return seedNotification(userId, title, null, false, null);
  }

  private Notification seedNotification(
      Long userId, String title, NotificationAction action) {
    return notificationRepository.save(
        Notification.builder()
            .userId(userId)
            .title(title)
            .content(title + "-content")
            .type(NotificationType.USER)
            .category(NotificationCategory.REVIEW)
            .action(action)
            .build());
  }

  private Notification seedRawAction(
      String actionType, Long targetId, Object payload, Short version) {
    Notification notification = seedNotification(USER_ID, "raw-action");
    ReflectionTestUtils.setField(notification, "actionType", actionType);
    ReflectionTestUtils.setField(notification, "actionTargetId", targetId);
    ReflectionTestUtils.setField(
        notification, "actionPayload", new ObjectMapper().valueToTree(payload));
    ReflectionTestUtils.setField(notification, "actionVersion", version);
    return notification;
  }

  private Notification seedNotification(
      Long userId,
      String title,
      NotificationStatus status,
      boolean isRead,
      LocalDateTime readAt) {
    return notificationRepository.save(
        Notification.builder()
            .userId(userId)
            .title(title)
            .content(title + "-content")
            .type(NotificationType.USER)
            .category(NotificationCategory.REVIEW)
            .status(status)
            .isRead(isRead)
            .readAt(readAt)
            .build());
  }
}
