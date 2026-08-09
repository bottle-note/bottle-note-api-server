package app.bottlenote.notification.constant;

import lombok.Getter;

/**
 * 클라이언트가 수행할 의미 기반 알림 동작을 정의한다.
 *
 * <p>서버에는 플랫폼별 URL이나 route 문자열 대신 이 값만 저장한다.
 */
@Getter
public enum NotificationActionType {
  OPEN_REVIEW("리뷰 상세 열기");

  private final String description;

  NotificationActionType(String description) {
    this.description = description;
  }
}
