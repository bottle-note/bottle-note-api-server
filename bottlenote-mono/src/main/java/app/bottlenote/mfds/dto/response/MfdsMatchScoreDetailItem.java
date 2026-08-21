package app.bottlenote.mfds.dto.response;

import java.math.BigDecimal;

/**
 * 알코올 후보 하나의 점수 근거. 각 요소는 0~1로 정규화되며, 비교할 데이터가 없어 계산에서 제외된 요소는 null이다.
 *
 * <p>totalScore는 존재하는 요소들의 가중 평균(가중치 재정규화)이다.
 */
public record MfdsMatchScoreDetailItem(
    BigDecimal nameScore,
    BigDecimal abvScore,
    BigDecimal ageScore,
    BigDecimal categoryScore,
    BigDecimal regionScore,
    BigDecimal totalScore) {}
