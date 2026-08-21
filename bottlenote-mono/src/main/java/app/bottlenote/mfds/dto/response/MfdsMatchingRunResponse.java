package app.bottlenote.mfds.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** 매칭 실행 결과. 저장된 상위 후보와 점수 근거를 담는다. */
public record MfdsMatchingRunResponse(
    Long declarationId,
    String matchingVersion,
    LocalDateTime matchedAt,
    List<MfdsAlcoholCandidateItem> alcoholCandidates,
    List<MfdsReferenceCandidateItem> distilleryCandidates,
    List<MfdsReferenceCandidateItem> regionCandidates) {}
