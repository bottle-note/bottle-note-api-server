package app.bottlenote.notification.event.listener;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationSourceType;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.fixture.InMemoryNotificationRepository;
import app.bottlenote.notification.fixture.InMemoryNotificationPreferenceRepository;
import app.bottlenote.notification.service.UserNotificationService;
import app.bottlenote.support.help.event.payload.HelpAnswerNotificationEvent;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] HelpAnswerNotificationListener")
class HelpAnswerNotificationListenerTest {

  private static final Long HELP_ID = 10L;
  private static final Long HELP_USER_ID = 20L;

  private InMemoryNotificationRepository notificationRepository;
  private HelpAnswerNotificationListener listener;

  private static HmacCursorCodec testCursorCodec() {
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("test-pagination-cursor-secret");
    return new HmacCursorCodec(properties, Clock.systemUTC());
  }

  @BeforeEach
  void setUp() {
    notificationRepository = new InMemoryNotificationRepository();
    listener =
        new HelpAnswerNotificationListener(
            new UserNotificationService(
                new FakeUserFacade(UserProfileItem.create(HELP_USER_ID, "문의 작성자", null)),
                notificationRepository,
                testCursorCodec(), new InMemoryNotificationPreferenceRepository()));
  }

  @Test
  @DisplayName("문의 답변 이벤트를 받으면 작성자에게 OPEN_HELP 알림을 저장한다")
  void handleHelpAnswerNotification_whenAnswered_savesOpenHelpNotification() {
    // given
    String content = "문의 답변 내용입니다.";
    HelpAnswerNotificationEvent event =
        HelpAnswerNotificationEvent.of(HELP_ID, HELP_USER_ID, content);

    // when
    listener.handleHelpAnswerNotification(event);

    // then
    assertThat(notificationRepository.findAll()).hasSize(1);
    Notification notification = notificationRepository.findAll().getFirst();
    assertThat(notification.getUserId()).isEqualTo(HELP_USER_ID);
    assertThat(notification.getType()).isEqualTo(NotificationType.USER);
    assertThat(notification.getCategory()).isEqualTo(NotificationCategory.ANSWER);
    assertThat(notification.getTitle()).isEqualTo(HelpAnswerNotificationListener.TITLE);
    assertThat(notification.getContent()).isEqualTo(content);
    assertThat(notification.getSourceType()).isEqualTo(NotificationSourceType.HELP_ANSWER.name());
    assertThat(notification.getSourceId()).isEqualTo(HELP_ID);
    assertThat(notification.getActionType()).isEqualTo(NotificationActionType.OPEN_HELP.name());
    assertThat(notification.getActionTargetId()).isEqualTo(HELP_ID);
    assertThat(notification.getActionPayload().isEmpty()).isTrue();
    assertThat(notification.getActionVersion()).isEqualTo((short) 1);
  }

  @Test
  @DisplayName("이벤트가 null이면 알림을 저장하지 않는다")
  void handleHelpAnswerNotification_whenEventNull_doesNothing() {
    // when
    listener.handleHelpAnswerNotification(null);

    // then
    assertThat(notificationRepository.findAll()).isEmpty();
  }
}
