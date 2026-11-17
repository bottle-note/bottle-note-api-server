package app.bottlenote.mfds.dto.response;

import java.math.BigDecimal;

/** 증류소·지역 후보 요약. */
public record MfdsReferenceCandidateItem(
    Long id, BigDecimal score, String korName, String engName) {}
