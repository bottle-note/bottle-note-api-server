package app.bottlenote.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
    service = new UserNotificationService(userFacade, notificationRepository);
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

      service.markAsRead(USER_ID, notification.getId());

      Notification saved = notificationRepository.findById(notification.getId()).orElseThrow();
      assertThat(saved.getIsRead()).isTrue();
      assertThat(saved.getReadAt()).isNotNull();
      assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("이미 읽은 알림을 다시 읽어도 최초 읽음 시각을 유지한다")
    void markAsRead_whenCalledAgain_preservesFirstReadAt() {
      Notification notification = seedNotification(USER_ID, "title");
      service.markAsRead(USER_ID, notification.getId());
      LocalDateTime firstReadAt = notification.getReadAt();

      service.markAsRead(USER_ID, notification.getId());

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

      service.markAsRead(USER_ID, notification.getId());

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
