package app.bottlenote.campaigncontent.constant;

import app.bottlenote.global.dto.response.AdminResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CampaignContentAdminResultCode implements AdminResultCode {
  CAMPAIGN_CONTENT_CREATED("캠페인 콘텐츠가 등록되었습니다."),
  CAMPAIGN_CONTENT_UPDATED("캠페인 콘텐츠가 수정되었습니다."),
  CAMPAIGN_CONTENT_DELETED("캠페인 콘텐츠가 삭제되었습니다."),
  CAMPAIGN_CONTENT_STATUS_UPDATED("캠페인 콘텐츠 활성화 상태가 변경되었습니다.");

  private final String message;
}
