package app.bottlenote.mfds.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매칭 확정 시 선택 근거. 후보 목록에서 골랐는지, 후보 밖 수동 지정인지, 자동 매칭 결과인지 구분한다. */
@Getter
@RequiredArgsConstructor
public enum MfdsMatchSelectionSource {
  CANDIDATE("후보 선택", "자동 매칭이 계산한 후보 목록에서 관리자가 선택한 경우에 해당 값이 사용된다"),
  MANUAL("직접 선택", "자동매칭이 아닌 관리자가 직접 선택한 경우에 해당 값이 사용된다"),
  AUTO("자동 매칭", "관리자 개입 없이 자동 매칭이 선정한 값이 그대로 확정된 경우에 해당 값이 사용된다");

  private final String name;
  private final String description;
}
