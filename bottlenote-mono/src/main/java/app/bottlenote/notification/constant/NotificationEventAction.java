package app.bottlenote.notification.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationEventAction {
  REVIEW_COMMENT(NotificationSettingGroup.REVIEW_AND_FOLLOW, "내 리뷰의 댓글", true),
  REVIEW_REPLY(NotificationSettingGroup.REVIEW_AND_FOLLOW, "내 댓글의 답글", true),
  REVIEW_LIKE(NotificationSettingGroup.REVIEW_AND_FOLLOW, "내 리뷰의 좋아요", true),
  FOLLOW(NotificationSettingGroup.REVIEW_AND_FOLLOW, "새 팔로워", true),
  FOLLOWING_REVIEW(NotificationSettingGroup.REVIEW_AND_FOLLOW, "팔로우한 사람의 새 리뷰", true),
  REVIEW_FEATURED(NotificationSettingGroup.REVIEW_AND_FOLLOW, "내 리뷰 추천·베스트 선정", true),
  TASTING_NEW(NotificationSettingGroup.TASTING, "새 시음회", true),
  TASTING_UPDATE(NotificationSettingGroup.TASTING, "시음회 변경·취소", true),
  TASTING_RESULT(NotificationSettingGroup.TASTING, "시음회 신청·참여 결과", true),
  PROGRAM_NEW(NotificationSettingGroup.PROGRAM, "새 프로그램", true),
  PROGRAM_UPDATE(NotificationSettingGroup.PROGRAM, "프로그램 변경·취소", true),
  PROGRAM_RESULT(NotificationSettingGroup.PROGRAM, "프로그램 신청·참여 결과", true),
  NOTICE(NotificationSettingGroup.NOTICE_AND_EVENT, "공지사항", true),
  CAMPAIGN(NotificationSettingGroup.NOTICE_AND_EVENT, "이벤트·캠페인", true),
  ADMIN_DIRECT(NotificationSettingGroup.NOTICE_AND_EVENT, "관리자 직접 안내", true),
  HELP_ANSWER(NotificationSettingGroup.MY_ACTIVITY, "문의 답변", true),
  REPORT_RESULT(NotificationSettingGroup.MY_ACTIVITY, "신고 처리 결과", true),
  CONTENT_MODERATION(NotificationSettingGroup.MY_ACTIVITY, "내 콘텐츠 숨김·삭제", true),
  ACCOUNT_STATUS(NotificationSettingGroup.MY_ACTIVITY, "계정 이용 제한·해제", true),
  ALCOHOL_SUBMISSION(NotificationSettingGroup.MY_ACTIVITY, "직접 등록 주류 승인·반려", true);

  private final NotificationSettingGroup group;
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
