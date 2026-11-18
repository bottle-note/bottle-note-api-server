package app.bottlenote.notification.constant;

import lombok.Getter;

/**
 * 알림 목록에서 사용할 읽음 상태 필터다.
 *
 * <p>전달 상태와 무관하게 {@code isRead} 값만 기준으로 조회한다.
 */
@Getter
public enum NotificationReadStatus {
  ALL("전체"),
  UNREAD("읽지 않음"),
  READ("읽음");

  private final String description;

  NotificationReadStatus(String description) {
    this.description = description;
  }
}
