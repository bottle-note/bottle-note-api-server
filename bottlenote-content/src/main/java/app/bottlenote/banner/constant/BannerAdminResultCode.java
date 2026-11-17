package app.bottlenote.banner.constant;

import app.bottlenote.global.dto.response.AdminResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BannerAdminResultCode implements AdminResultCode {
  BANNER_CREATED("배너가 등록되었습니다."),
  BANNER_UPDATED("배너가 수정되었습니다."),
  BANNER_DELETED("배너가 삭제되었습니다."),
  BANNER_STATUS_UPDATED("배너 활성화 상태가 변경되었습니다."),
  BANNER_SORT_ORDER_UPDATED("배너 정렬 순서가 변경되었습니다.");

  private final String message;
}
