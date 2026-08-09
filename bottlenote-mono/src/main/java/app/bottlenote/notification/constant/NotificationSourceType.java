package app.bottlenote.notification.constant;

import lombok.Getter;

/**
 * 알림 생성을 유발한 원본 이벤트 유형이다.
 *
 * <p>원본 식별자와 수신 사용자 조합으로 동일 이벤트의 중복 저장을 방지한다.
 */
@Getter
public enum NotificationSourceType {
  REVIEW_REPLY("리뷰 댓글");

  private final String description;

  NotificationSourceType(String description) {
    this.description = description;
  }
}
