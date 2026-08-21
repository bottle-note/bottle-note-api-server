package app.bottlenote.mfds.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MfdsImporterAdminStatus {
  ACTIVE("활성", "관리 및 노출 대상 수입사"),
  INACTIVE("비활성", "관리 및 노출에서 제외한 수입사");

  private final String name;
  private final String description;
}
