package app.bottlenote.notification.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationEventAction {
  REVIEW_COMMENT(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰의 댓글",
      "내 삭제되지 않은 리뷰에 다른 사용자가 댓글을 작성한 경우 발생하는 알림",
      true),
  REVIEW_REPLY(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 댓글의 답글",
      "내 삭제되지 않은 댓글에 다른 사용자가 답글을 작성한 경우 발생하는 알림",
      true),
  REVIEW_LIKE(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰의 좋아요",
      "내 삭제되지 않은 리뷰에 다른 사용자가 좋아요를 누른 경우 발생하는 알림",
      true),
  FOLLOW(NotificationSettingGroup.REVIEW_AND_FOLLOW, "새 팔로워", "다른 사용자가 나를 팔로우한 경우 발생하는 알림", true),
  FOLLOWING_REVIEW(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "팔로우한 사람의 새 리뷰",
      "내가 팔로우한 사용자가 새 리뷰를 작성한 경우 발생하는 알림",
      true),
  REVIEW_FEATURED(
      NotificationSettingGroup.REVIEW_AND_FOLLOW,
      "내 리뷰 추천·베스트 선정",
      "내 리뷰가 추천 리뷰 또는 베스트 리뷰로 선정된 경우 발생하는 알림",
      true),
  TASTING_NEW(NotificationSettingGroup.TASTING, "새 시음회", "새로운 시음회 모집이 공개된 경우 발생하는 알림", true),
  TASTING_UPDATE(
      NotificationSettingGroup.TASTING,
      "시음회 변경·취소",
      "내가 신청한 시음회의 안내 사항이 변경되거나 시음회가 취소된 경우 발생하는 알림",
      true),
  TASTING_RESULT(
      NotificationSettingGroup.TASTING,
      "시음회 신청·참여 결과",
      "내가 신청한 시음회의 신청 또는 참여 결과가 안내되는 경우 발생하는 알림",
      true),
  PROGRAM_NEW(NotificationSettingGroup.PROGRAM, "새 프로그램", "새로운 프로그램 모집이 공개된 경우 발생하는 알림", true),
  PROGRAM_UPDATE(
      NotificationSettingGroup.PROGRAM,
      "프로그램 변경·취소",
      "내가 신청한 프로그램의 안내 사항이 변경되거나 프로그램이 취소된 경우 발생하는 알림",
      true),
  PROGRAM_RESULT(
      NotificationSettingGroup.PROGRAM,
      "프로그램 신청·참여 결과",
      "내가 신청한 프로그램의 신청 또는 참여 결과가 안내되는 경우 발생하는 알림",
      true),
  NOTICE(NotificationSettingGroup.NOTICE_AND_EVENT, "공지사항", "서비스 공지사항이 알림으로 발행된 경우 발생하는 알림", true),
  CAMPAIGN(
      NotificationSettingGroup.NOTICE_AND_EVENT,
      "이벤트·캠페인",
      "이벤트 또는 캠페인 소식이 알림으로 발행된 경우 발생하는 알림",
      true),
  ADMIN_DIRECT(
      NotificationSettingGroup.NOTICE_AND_EVENT,
      "관리자 직접 안내",
      "관리자가 나를 수신 대상으로 지정하여 안내를 발송한 경우 발생하는 알림",
      true),
  HELP_ANSWER(
      NotificationSettingGroup.MY_ACTIVITY, "문의 답변", "내가 등록한 문의에 관리자 답변이 등록된 경우 발생하는 알림", true),
  REPORT_RESULT(
      NotificationSettingGroup.MY_ACTIVITY, "신고 처리 결과", "내가 접수한 신고의 처리 결과가 안내되는 경우 발생하는 알림", true),
  CONTENT_MODERATION(
      NotificationSettingGroup.MY_ACTIVITY,
      "내 콘텐츠 숨김·삭제",
      "내가 작성한 콘텐츠가 운영 정책에 따라 숨김 또는 삭제 처리된 경우 발생하는 알림",
      true),
  ACCOUNT_STATUS(
      NotificationSettingGroup.MY_ACTIVITY,
      "계정 이용 제한·해제",
      "내 계정의 이용이 제한되거나 기존 이용 제한이 해제된 경우 발생하는 알림",
      true),
  ALCOHOL_SUBMISSION(
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

  // 기존 원본 키를 유지해 과거 이벤트 재전달 시 중복 생성을 방지한다.
  public String sourceType() {
    return this == REVIEW_COMMENT ? REVIEW_REPLY.name() : name();
  }
}
