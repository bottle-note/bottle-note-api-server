package app.bottlenote.mfds.dto.response;

import java.math.BigDecimal;

/** 알코올 후보 요약. scoreDetail은 매칭 실행 응답에서만 채워지고 저장 후보 조회에서는 null이다. */
public record MfdsAlcoholCandidateItem(
    Long alcoholId,
    BigDecimal score,
    String korName,
    String engName,
    String korCategory,
    String engCategory,
    String abv,
    String age,
    String imageUrl,
    MfdsMatchScoreDetailItem scoreDetail) {}
