package app.bottlenote.mfds.constant;

import app.bottlenote.global.dto.response.AdminResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfdsAdminResultCode implements AdminResultCode {
  MFDS_IMPORTER_CREATED("수입사가 등록되었습니다."),
  MFDS_IMPORTER_UPDATED("수입사가 수정되었습니다."),
  MFDS_IMPORTER_DELETED("수입사가 삭제되었습니다."),
  MFDS_DECLARATION_STATUS_UPDATED("수입 신고 정규화 상태가 변경되었습니다."),
  MFDS_DECLARATION_IMPORTER_LINKED("수입 신고에 수입사가 연결되었습니다."),
  MFDS_DECLARATION_IMPORTER_UNLINKED("수입 신고의 수입사 연결이 해제되었습니다."),
  MFDS_RCNO_LINK_CREATED("수입신고번호 연결 근거가 등록되었습니다."),
  MFDS_RCNO_LINK_DELETED("수입신고번호 연결 근거가 삭제되었습니다.");

  private final String message;
}
