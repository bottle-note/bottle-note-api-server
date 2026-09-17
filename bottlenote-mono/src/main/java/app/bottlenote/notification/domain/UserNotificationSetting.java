package app.bottlenote.notification.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.notification.constant.NotificationEventAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
    name = "user_notification_settings",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_notification_settings_user_action",
            columnNames = {"user_id", "action_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationSetting extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("알림 수신 사용자 ID")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_code", nullable = false, length = 64)
  @Comment("알림 발생 액션 코드")
  private NotificationEventAction actionCode;

  @Column(nullable = false)
  @Comment("앱 내부 알림 수신 허용 여부")
  private boolean enabled;

  public UserNotificationSetting(Long userId, NotificationEventAction actionCode, boolean enabled) {
    this.userId = Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
    if (userId <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
    }
    this.actionCode = Objects.requireNonNull(actionCode, "알림 발생 액션은 필수입니다.");
    changeEnabled(enabled);
  }

  public void changeEnabled(boolean enabled) {
    if (enabled == actionCode.isDefaultEnabled()) {
      throw new IllegalArgumentException("기본값과 같은 설정은 저장하지 않습니다.");
    }
    this.enabled = enabled;
  }
}
