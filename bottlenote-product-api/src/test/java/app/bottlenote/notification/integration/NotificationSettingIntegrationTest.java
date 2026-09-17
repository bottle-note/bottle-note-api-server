package app.bottlenote.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.payload.NotificationMessage;
import app.bottlenote.notification.repository.JpaNotificationRepository;
import app.bottlenote.notification.service.NotificationService;
import app.bottlenote.notification.service.NotificationSettingService;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("integration")
@DisplayName("알림 수신 설정 DB 매핑")
class NotificationSettingIntegrationTest extends IntegrationTestSupport {
  @Autowired private NotificationSettingService settings;
  @Autowired private NotificationService notifications;
  @Autowired private JpaNotificationRepository repository;
  @Autowired private UserTestFactory users;
  @Autowired private JdbcTemplate jdbc;

  @ParameterizedTest
  @EnumSource(NotificationEventAction.class)
  @DisplayName("설정 변경 시 enum 문자열과 변경값만 DB에 저장하고 기본값 복원 시 삭제한다")
  void 설정_저장과_복원을_검증한다(NotificationEventAction action) {
    Long userId = users.persistUser().getId();
    assertThat(settings.isEnabled(userId, action)).isEqualTo(action.isDefaultEnabled());
    settings.changeSetting(userId, action, !action.isDefaultEnabled());
    settings.changeSetting(userId, action, !action.isDefaultEnabled());
    assertThat(
            jdbc.queryForObject(
                "SELECT action_code FROM user_notification_settings WHERE user_id = ?",
                String.class,
                userId))
        .isEqualTo(action.name());
    assertThat(
            jdbc.queryForObject(
                "SELECT enabled FROM user_notification_settings WHERE user_id = ?",
                Boolean.class,
                userId))
        .isEqualTo(!action.isDefaultEnabled());
    assertThat(settings.isEnabled(userId, action)).isEqualTo(!action.isDefaultEnabled());
    settings.changeSetting(userId, action, action.isDefaultEnabled());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_notification_settings WHERE user_id = ?",
                Long.class,
                userId))
        .isZero();
    assertThat(settings.isEnabled(userId, action)).isEqualTo(action.isDefaultEnabled());
  }

  @ParameterizedTest
  @EnumSource(
      value = NotificationEventAction.class,
      names = {"REVIEW_COMMENT", "REVIEW_REPLY", "REVIEW_LIKE", "FOLLOW", "HELP_ANSWER"})
  @DisplayName("기존 발행 경로에서 DB 거부 설정과 허용 복원을 반영하고 발생 액션을 저장한다")
  void 기존_발행과_DB_설정을_연결한다(NotificationEventAction action) {
    Long userId = users.persistUser().getId();
    NotificationMessage message =
        switch (action) {
          case REVIEW_COMMENT -> NotificationMessage.reviewReply(userId, 10L, 20L, "댓글", "내용");
          case REVIEW_REPLY ->
              NotificationMessage.reviewReplyResponse(userId, 10L, 20L, "답글", "내용");
          case REVIEW_LIKE -> NotificationMessage.reviewLike(userId, 10L, 20L, "좋아요", "내용");
          case FOLLOW -> NotificationMessage.follow(userId, 10L, 20L, "팔로우", "내용");
          case HELP_ANSWER -> NotificationMessage.helpAnswer(userId, 20L, "문의", "내용");
          default -> throw new IllegalArgumentException("기존 발행 액션이 아닙니다.");
        };
    settings.changeSetting(userId, action, false);
    notifications.sendNotification(message);
    assertThat(repository.countByUserId(userId)).isZero();
    settings.changeSetting(userId, action, true);
    notifications.sendNotification(message);
    notifications.sendNotification(message);
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            n -> {
              assertThat(n.getEventAction()).isEqualTo(action);
              assertThat(n.getSourceType()).isEqualTo(action.sourceType());
              assertThat(n.getSourceId()).isEqualTo(20L);
              assertThat(n.getActionType()).isEqualTo(message.action().type().name());
            });
    assertThat(
            jdbc.queryForObject(
                "SELECT event_action FROM notifications WHERE user_id = ?", String.class, userId))
        .isEqualTo(action.name());
  }

  @Test
  @DisplayName("다른 사용자 또는 다른 액션의 거부 설정은 현재 알림 생성을 차단하지 않는다")
  void 설정의_사용자와_액션을_격리한다() {
    Long userId = users.persistUser().getId();
    Long otherId = users.persistUser().getId();
    settings.changeSetting(otherId, NotificationEventAction.REVIEW_COMMENT, false);
    settings.changeSetting(userId, NotificationEventAction.REVIEW_REPLY, false);
    notifications.sendNotification(NotificationMessage.reviewReply(userId, 10L, 20L, "댓글", "내용"));
    assertThat(repository.countByUserId(userId)).isEqualTo(1);
  }

  @Test
  @DisplayName("동일 원본 키로 재발행해도 중복 저장하지 않는다")
  void 원본_키로_중복_저장을_방지한다() {
    Long userId = users.persistUser().getId();
    jdbc.update(
        "INSERT INTO notifications (user_id, title, content, event_action, status, is_read, source_type, source_id) VALUES (?, '기존 댓글', '내용', 'REVIEW_COMMENT', 'PENDING', false, 'REVIEW_REPLY', 20)",
        userId);
    notifications.sendNotification(NotificationMessage.reviewReply(userId, 10L, 20L, "댓글", "내용"));
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            n -> {
              assertThat(n.getEventAction()).isEqualTo(NotificationEventAction.REVIEW_COMMENT);
              assertThat(n.getTitle()).isEqualTo("기존 댓글");
            });
  }

  @Test
  @DisplayName("동일 설정을 동시에 생성할 때 유일 키 충돌 없이 한 행을 저장한다")
  void 동시_설정_변경을_저장한다() throws Exception {
    Long userId = users.persistUser().getId();
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Void> change =
          () -> {
            settings.changeSetting(userId, NotificationEventAction.REVIEW_LIKE, false);
            return null;
          };
      for (var future : executor.invokeAll(List.of(change, change))) future.get();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_notification_settings WHERE user_id = ?",
                Long.class,
                userId))
        .isEqualTo(1);
    assertThat(settings.isEnabled(userId, NotificationEventAction.REVIEW_LIKE)).isFalse();
  }

  @Test
  @DisplayName("사용자와 액션이 중복인 행을 직접 추가할 때 DB가 거부한다")
  void DB_유일_키를_검증한다() {
    Long userId = users.persistUser().getId();
    settings.changeSetting(userId, NotificationEventAction.FOLLOW, false);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO user_notification_settings (user_id, action_code, enabled) VALUES (?, 'FOLLOW', false)",
                    userId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
