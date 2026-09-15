package app.bottlenote.campaigncontent.domain;

/**
 * 캠페인 콘텐츠 이벤트 집계 결과.
 *
 * <p>진입·시작·완료는 방문자, 결과 조회는 회원 기준이다. 비율 계산에 쓰는 교집합은 같은 방문자 안에서 센다.
 */
public record CampaignContentEventCounts(
    long viewVisitors,
    long startVisitors,
    long finishVisitors,
    long resultMembers,
    long startedAndFinishedVisitors,
    long finishedAndResultVisitors) {

  public static CampaignContentEventCounts empty() {
    return new CampaignContentEventCounts(0L, 0L, 0L, 0L, 0L, 0L);
  }
}
