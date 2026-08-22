package app.bottlenote.mfds.dto.response;

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

  /** 선택 상태. ID 필드는 관리자가 확정해야 채워지지만, 매칭 근거 필드에는 확정 전에도 정규화 배치가 남긴 판정 값이 있을 수 있다. 확정 여부는 ID로 판단한다. */
  public record MfdsMatchingSelection(
      Long alcoholId,
      String alcoholMatchDecision,
      Long distilleryId,
      String distilleryMatchSource,
      Long regionId,
      String regionMatchSource) {}
}
