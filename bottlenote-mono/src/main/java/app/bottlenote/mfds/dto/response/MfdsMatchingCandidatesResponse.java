package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import java.time.LocalDateTime;
import java.util.List;

/** 선언에 저장된 매칭 후보와 확정 상태 조회 결과. */
public record MfdsMatchingCandidatesResponse(
    Long declarationId,
    String matchingVersion,
    LocalDateTime matchedAt,
    MfdsMatchingSelection selection,
    List<MfdsAlcoholCandidateItem> alcoholCandidates,
    List<MfdsReferenceCandidateItem> distilleryCandidates,
    List<MfdsReferenceCandidateItem> regionCandidates) {

  /** 확정된 선택 상태. 확정 전이면 모든 필드가 null이다. */
  public record MfdsMatchingSelection(
      Long alcoholId,
      MfdsMatchSelectionSource alcoholMatchDecision,
      Long distilleryId,
      MfdsMatchSelectionSource distilleryMatchSource,
      Long regionId,
      MfdsMatchSelectionSource regionMatchSource) {}
}
