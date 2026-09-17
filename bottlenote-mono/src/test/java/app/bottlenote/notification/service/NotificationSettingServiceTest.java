package app.bottlenote.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.constant.NotificationSettingGroup;
import app.bottlenote.notification.domain.UserNotificationSetting;
import app.bottlenote.notification.fixture.InMemoryUserNotificationSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("unit")
class NotificationSettingServiceTest {
  private final InMemoryUserNotificationSettingRepository repository =
      new InMemoryUserNotificationSettingRepository();
  private final NotificationSettingService service = new NotificationSettingService(repository);

  @ParameterizedTest
  @EnumSource(NotificationEventAction.class)
  @DisplayName("설정 행이 없을 때 액션 기본값을 적용하고 행을 만들지 않는다")
  void 기본값을_적용한다(NotificationEventAction action) {
    assertThat(service.isEnabled(1L, action)).isEqualTo(action.isDefaultEnabled());
    assertThat(repository.findByUserIdAndActionCode(1L, action)).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(NotificationEventAction.class)
  @DisplayName("기본값과 다른 설정을 반복 저장할 때 해당 사용자와 액션에만 적용한다")
  void 변경값을_저장한다(NotificationEventAction action) {
    service.changeSetting(1L, action, !action.isDefaultEnabled());
    service.changeSetting(1L, action, !action.isDefaultEnabled());
    assertThat(service.isEnabled(1L, action)).isEqualTo(!action.isDefaultEnabled());
    assertThat(service.isEnabled(2L, action)).isEqualTo(action.isDefaultEnabled());
    for (NotificationEventAction other : NotificationEventAction.values()) {
      if (other != action)
        assertThat(service.isEnabled(1L, other)).isEqualTo(other.isDefaultEnabled());
    }
  }

  @ParameterizedTest
  @EnumSource(NotificationEventAction.class)
  @DisplayName("기본값으로 복원할 때 기존 설정 행을 삭제한다")
  void 기본값으로_복원한다(NotificationEventAction action) {
    service.changeSetting(1L, action, !action.isDefaultEnabled());
    service.changeSetting(1L, action, action.isDefaultEnabled());
    assertThat(repository.findByUserIdAndActionCode(1L, action)).isEmpty();
    assertThat(service.isEnabled(1L, action)).isEqualTo(action.isDefaultEnabled());
  }

  @Test
  @DisplayName("리뷰와 팔로우 그룹을 조회할 때 소속된 여섯 액션을 반환한다")
  void 그룹으로_액션을_찾는다() {
    assertThat(NotificationEventAction.findByGroup(NotificationSettingGroup.REVIEW_AND_FOLLOW))
        .containsExactly(
            NotificationEventAction.REVIEW_COMMENT,
            NotificationEventAction.REVIEW_REPLY,
            NotificationEventAction.REVIEW_LIKE,
            NotificationEventAction.FOLLOW,
            NotificationEventAction.FOLLOWING_REVIEW,
            NotificationEventAction.REVIEW_FEATURED);
  }

  @Test
  @DisplayName("기본값을 설정 엔티티로 저장하려 할 때 거부한다")
  void 기본값_행을_거부한다() {
    assertThatThrownBy(
            () -> new UserNotificationSetting(1L, NotificationEventAction.REVIEW_COMMENT, true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> service.changeSetting(0L, NotificationEventAction.REVIEW_COMMENT, false))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
