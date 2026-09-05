package app.bottlenote.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.notification.constant.NotificationKind;
import app.bottlenote.notification.dto.request.NotificationPreferenceRequest;
import app.bottlenote.notification.exception.NotificationException;
import app.bottlenote.notification.fixture.InMemoryNotificationPreferenceRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("인앱 알림 수신 설정")
class NotificationPreferenceServiceTest {
  private DefaultNotificationPreferenceService service;

  @BeforeEach
  void setUp() {
    service = new DefaultNotificationPreferenceService(new InMemoryNotificationPreferenceRepository(),
        new FakeUserFacade(UserProfileItem.create(1L, "작성자", null), UserProfileItem.create(2L, "다른 사용자", null)));
  }

  @Test
  @DisplayName("처음 조회할 때 여섯 유형을 모두 허용한다")
  void 처음에는_모두_허용한다() {
    assertThat(service.getPreferences(1L).settings()).hasSize(6).containsOnlyValues(true);
  }

  @Test
  @DisplayName("일부 유형을 변경할 때 다른 유형과 다른 사용자 설정을 유지한다")
  void 일부_설정만_변경한다() {
    service.updatePreferences(1L, new NotificationPreferenceRequest(Map.of(NotificationKind.FOLLOW, false)));
    service.updatePreferences(1L, new NotificationPreferenceRequest(Map.of(NotificationKind.REVIEW_LIKE, false)));
    assertThat(service.getPreferences(1L).settings())
        .containsEntry(NotificationKind.FOLLOW, false)
        .containsEntry(NotificationKind.REVIEW_LIKE, false)
        .containsEntry(NotificationKind.HELP_ANSWER, true);
    assertThat(service.getPreferences(2L).settings()).containsOnlyValues(true);
  }

  @Test
  @DisplayName("같은 설정을 반복하거나 다시 허용할 때 최종 상태를 반환한다")
  void 반복_설정을_처리한다() {
    var off = new NotificationPreferenceRequest(Map.of(NotificationKind.REVIEW_LIKE, false));
    assertThat(service.updatePreferences(1L, off)).isEqualTo(service.updatePreferences(1L, off));
    assertThat(service.updatePreferences(1L,
        new NotificationPreferenceRequest(Map.of(NotificationKind.REVIEW_LIKE, true))).settings())
        .containsOnlyValues(true);
  }

  @Test
  @DisplayName("빈 설정이나 null 설정을 변경할 때 거부한다")
  void 잘못된_설정을_거부한다() {
    assertThatThrownBy(() -> service.updatePreferences(1L, null)).isInstanceOf(NotificationException.class);
    assertThatThrownBy(() -> service.updatePreferences(1L, new NotificationPreferenceRequest(null)))
        .isInstanceOf(NotificationException.class);
    assertThatThrownBy(() -> service.updatePreferences(1L, new NotificationPreferenceRequest(Map.of())))
        .isInstanceOf(NotificationException.class);
    Map<NotificationKind, Boolean> settings = new HashMap<>();
    settings.put(NotificationKind.FOLLOW, null);
    assertThatThrownBy(() -> service.updatePreferences(1L, new NotificationPreferenceRequest(settings)))
        .isInstanceOf(NotificationException.class);
  }

  @Test
  @DisplayName("존재하지 않는 사용자가 조회하거나 변경할 때 거부한다")
  void 없는_사용자를_거부한다() {
    assertThatThrownBy(() -> service.getPreferences(3L)).isInstanceOf(UserException.class);
    assertThatThrownBy(() -> service.updatePreferences(3L,
        new NotificationPreferenceRequest(Map.of(NotificationKind.FOLLOW, false))))
        .isInstanceOf(UserException.class);
  }
}
