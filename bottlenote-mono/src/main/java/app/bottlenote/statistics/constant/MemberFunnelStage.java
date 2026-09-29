package app.bottlenote.statistics.constant;

import lombok.Getter;

/** 회원 전환 퍼널 단계. 선언 순서가 퍼널 순서다. */
@Getter
public enum MemberFunnelStage {
  VISITED("방문"),
  SIGNED_UP("가입"),
  AGREED("필수 약관 동의"),
  ACTIVATED("첫 행동");

  private final String label;

  MemberFunnelStage(String label) {
    this.label = label;
  }
}
