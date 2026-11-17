package app.bottlenote.campaigncontent.dto.response;

import java.time.LocalDate;

/**
 * 캠페인 콘텐츠 참여 지표. 비율은 백분율 소수 1자리이고 분모가 0이면 0이다.
 *
 * @param viewVisitors 진입 순방문자
 * @param startVisitors 시작 순방문자
 * @param finishVisitors 완료 순방문자
 * @param resultMembers 결과를 조회한 순회원
 * @param activeMembers 같은 기간 서비스 전체 활성 회원
 * @param completionRate 시작한 방문자 중 완료한 방문자 비율
 * @param loginConversionRate 완료한 방문자 중 결과까지 조회한 방문자 비율
 * @param participationRate 활성 회원 중 결과를 조회한 회원 비율
 */
public record AdminCampaignContentMetricsResponse(
    LocalDate from,
    LocalDate to,
    long viewVisitors,
    long startVisitors,
    long finishVisitors,
    long resultMembers,
    long activeMembers,
    double completionRate,
    double loginConversionRate,
    double participationRate) {}
