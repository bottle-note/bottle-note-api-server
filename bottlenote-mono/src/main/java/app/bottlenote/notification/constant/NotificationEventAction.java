package app.bottlenote.notification.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationEventAction {
  REVIEW_COMMENT_CREATE(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰의 댓글",
      "내 삭제되지 않은 리뷰에 다른 사용자가 댓글을 작성한 경우 발생하는 알림",
      true),
  REVIEW_REPLY_CREATE(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 댓글의 답글",
      "내 삭제되지 않은 댓글에 다른 사용자가 답글을 작성한 경우 발생하는 알림",
      true),
  REVIEW_LIKE_ADD(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰의 좋아요",
      "내 삭제되지 않은 리뷰에 다른 사용자가 좋아요를 누른 경우 발생하는 알림",
      true),
  FOLLOW_CREATE(NotificationSettingGroup.REVIEW_AND_FOLLOW, "새 팔로워", "다른 사용자가 나를 팔로우한 경우 발생하는 알림", true),
  FOLLOWING_REVIEW_CREATE(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "팔로우한 사람의 새 리뷰",
      "내가 팔로우한 사용자가 새 리뷰를 작성한 경우 발생하는 알림",
      true),
  REVIEW_FEATURE_SELECT(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰 추천·베스트 선정",
      "내 리뷰가 추천 리뷰 또는 베스트 리뷰로 선정된 경우 발생하는 알림",
      true),
  TASTING_OPEN(NotificationSettingGroup.TASTING, "새 시음회", "새로운 시음회 모집이 공개된 경우 발생하는 알림", true),
  TASTING_UPDATE(
      NotificationSettingGroup.TASTING,
      "시음회 변경·취소",
      "내가 신청한 시음회의 안내 사항이 변경되거나 시음회가 취소된 경우 발생하는 알림",
      true),
  PROGRAM_OPEN(NotificationSettingGroup.PROGRAM, "새 프로그램", "새로운 프로그램 모집이 공개된 경우 발생하는 알림", true),
  PROGRAM_UPDATE(
      NotificationSettingGroup.PROGRAM,
      "프로그램 변경·취소",
      "내가 신청한 프로그램의 안내 사항이 변경되거나 프로그램이 취소된 경우 발생하는 알림",
      true),
  PROGRAM_RESULT_ANNOUNCE(
      NotificationSettingGroup.PROGRAM,
      "프로그램 신청·참여 결과",
      "내가 신청한 프로그램의 신청 또는 참여 결과가 안내되는 경우 발생하는 알림",
      true),
  NOTICE_PUBLISH(
      NotificationSettingGroup.NOTICE_AND_EVENT, "공지사항", "서비스 공지사항이 알림으로 발행된 경우 발생하는 알림", true),
  CAMPAIGN_OPEN(
      NotificationSettingGroup.NOTICE_AND_EVENT,
      "이벤트·캠페인",
      "이벤트 또는 캠페인 소식이 알림으로 발행된 경우 발생하는 알림",
      true),
  HELP_ANSWER_CREATE(
      NotificationSettingGroup.MY_ACTIVITY, "문의 답변", "내가 등록한 문의에 관리자 답변이 등록된 경우 발생하는 알림", true),
  REPORT_RESULT_ANNOUNCE(
      NotificationSettingGroup.MY_ACTIVITY, "신고 처리 결과", "내가 접수한 신고의 처리 결과가 안내되는 경우 발생하는 알림", true),
  CONTENT_MODERATE(
      NotificationSettingGroup.MY_ACTIVITY,
      "내 콘텐츠 숨김·삭제",
      "내가 작성한 콘텐츠가 운영 정책에 따라 숨김 또는 삭제 처리된 경우 발생하는 알림",
      true),
  ACCOUNT_STATUS_UPDATE(
      NotificationSettingGroup.MY_ACTIVITY,
      "계정 이용 제한·해제",
      "내 계정의 이용이 제한되거나 기존 이용 제한이 해제된 경우 발생하는 알림",
      true),
  ALCOHOL_SUBMISSION_REVIEW(
      NotificationSettingGroup.MY_ACTIVITY,
      "직접 등록 주류 승인·반려",
      "내가 직접 등록 요청한 주류가 승인 또는 반려된 경우 발생하는 알림",
      true);

  private final NotificationSettingGroup group;
  private final String displayName;
  private final String description;
  private final boolean defaultEnabled;

  public static List<NotificationEventAction> findByGroup(NotificationSettingGroup group) {
    Objects.requireNonNull(group, "알림 설정 그룹은 필수입니다.");
    return Arrays.stream(values()).filter(action -> action.group == group).toList();
  }

  // 댓글/답글은 같은 replyId 원본 키를 공유해 동일 수신자 중복 저장을 막는다.
  public String sourceType() {
    return this == REVIEW_COMMENT_CREATE ? REVIEW_REPLY_CREATE.name() : name();
  }
}
