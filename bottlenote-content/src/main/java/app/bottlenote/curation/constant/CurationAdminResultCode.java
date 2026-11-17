package app.bottlenote.curation.constant;

import app.bottlenote.global.dto.response.AdminResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurationAdminResultCode implements AdminResultCode {
  CURATION_CREATED("큐레이션이 등록되었습니다."),
  CURATION_UPDATED("큐레이션이 수정되었습니다.");

  private final String message;
}
