package app.bottlenote.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.fixture.InMemoryNotificationRepository;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
@DisplayName("알림 생성 정책")
class NotificationCreationServiceTest {
  private InMemoryNotificationRepository repository;
  private UserNotificationService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryNotificationRepository();
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("notification-test-cursor-secret");
    service =
        new UserNotificationService(
            new FakeUserFacade(UserProfileItem.create(1L, "사용자", null)),
            repository,
            new HmacCursorCodec(properties, Clock.systemUTC()));
  }

  @ParameterizedTest
  @MethodSource("messages")
  @DisplayName("같은 사건을 재전달할 때 기존 알림과 읽음 상태를 보존한다")
  void 재전달에_중복_알림을_만들지_않는다(NotificationMessage message) {
    service.sendNotification(message);
    repository.findAll().getFirst().markAsRead();
    service.sendNotification(message);
    assertThat(repository.findAll()).hasSize(1);
    assertThat(repository.findAll().getFirst().getIsRead()).isTrue();
  }

  @Test
  @DisplayName("같은 사용자가 리뷰와 부모 댓글 작성자일 때 두 경로를 하나로 저장한다")
  void 댓글_수신_경로를_중복_제거한다() {
    service.sendNotification(NotificationMessage.reviewReply(1L, 2L, 3L, "댓글", "내용"));
    service.sendNotification(NotificationMessage.reviewReplyResponse(1L, 2L, 3L, "답글", "내용"));
    assertThat(repository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("새 Action을 만들 때 기존 댓글과 문의 계약을 유지한다")
  void 새_Action의_타입과_버전을_검증한다() {
    assertThat(NotificationAction.openReview(2L, 3L).version()).isEqualTo(1);
    assertThat(NotificationAction.openHelp(2L).version()).isEqualTo(1);
    assertThat(NotificationAction.openReview(2L).version()).isEqualTo(2);
    assertThat(NotificationAction.openReview(2L).payload().isEmpty()).isTrue();
    assertThat(NotificationAction.openUser(2L).type()).isEqualTo(NotificationActionType.OPEN_USER);
    assertThatThrownBy(
            () ->
                new NotificationAction(
                    NotificationActionType.OPEN_REVIEW,
                    2L,
                    JsonNodeFactory.instance.objectNode().put("replyId", 3L),
                    2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new NotificationAction(
                    NotificationActionType.OPEN_USER, 2L, JsonNodeFactory.instance.objectNode(), 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NotificationAction.openUser(0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static java.util.stream.Stream<NotificationMessage> messages() {
    return java.util.stream.Stream.of(
        NotificationMessage.reviewReply(1L, 2L, 10L, "댓글", "내용"),
        NotificationMessage.reviewReplyResponse(1L, 2L, 10L, "답글", "내용"),
        NotificationMessage.reviewLike(1L, 2L, 10L, "좋아요", "내용"),
        NotificationMessage.follow(1L, 2L, 10L, "팔로우", "내용"),
        NotificationMessage.helpAnswer(1L, 10L, "문의 답변", "내용"));
  }
}
