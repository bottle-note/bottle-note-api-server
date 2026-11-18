package app.bottlenote.notification.constant;

import lombok.Getter;

/**
 * 알림 Action을 수행할 수 없을 때 사용할 공통 대체 동작이다.
 *
 * <p>앱과 웹은 이 값을 각 플랫폼의 내부 화면으로 변환한다.
 */
@Getter
public enum NotificationActionFallbackType {
  OPEN_NOTIFICATION_CENTER("알림함 열기");

  private final String description;

  NotificationActionFallbackType(String description) {
    this.description = description;
  }
}
