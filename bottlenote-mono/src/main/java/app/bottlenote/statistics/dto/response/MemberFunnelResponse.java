package app.bottlenote.statistics.dto.response;

import app.bottlenote.statistics.constant.MemberFunnelStage;
import java.util.List;

/**
 * 회원 전환 퍼널. 단계는 선언 순서대로 내려가며 앞 단계 대비 전환율과 방문 대비 누적 전환율을 함께 내린다.
 *
 * @param from 구간 시작 (ISO 로컬 일시)
 * @param to 구간 끝 버킷 시작 (ISO 로컬 일시)
 */
public record MemberFunnelResponse(
    String from, String to, String timezone, List<StageItem> stages) {

  public MemberFunnelResponse {
    stages = stages == null ? List.of() : List.copyOf(stages);
  }

  /**
   * @param conversionRate 직전 단계 대비 비율(%). 첫 단계는 null
   * @param overallRate 첫 단계 대비 비율(%). 첫 단계는 100.0, 첫 단계가 0이면 0.0
   */
  public record StageItem(
      MemberFunnelStage stage,
      String label,
      long count,
      Double conversionRate,
      double overallRate) {}
}
