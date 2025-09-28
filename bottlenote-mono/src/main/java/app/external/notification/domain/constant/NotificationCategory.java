package app.external.notification.domain.constant;

import lombok.Getter;

@Getter
public enum NotificationCategory {
  REVIEW("리뷰"),
  NOTICE("공지사항"),
  QUESTION("문의"),
  ANSWER("답변");

  private final String description;

  NotificationCategory(String description) {
    this.description = description;
  }
}
