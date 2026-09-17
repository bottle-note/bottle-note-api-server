package app.bottlenote.notification.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationSettingGroup {
  REVIEW_AND_FOLLOW("리뷰와 팔로우"),
  TASTING("시음회 소식"),
  PROGRAM("프로그램 소식"),
  NOTICE_AND_EVENT("공지와 이벤트"),
  MY_ACTIVITY("문의와 내 활동");

  private final String description;
}
