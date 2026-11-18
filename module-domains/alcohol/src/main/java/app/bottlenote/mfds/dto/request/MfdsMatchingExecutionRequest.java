package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.dto.response.MfdsAlcoholCandidateItem;
import app.bottlenote.mfds.dto.response.MfdsReferenceCandidateItem;
import java.time.LocalDateTime;
import java.util.List;

/** 한 번의 매칭 계산 결과를 영속화 계층으로 전달한다. */
public record MfdsMatchingExecutionRequest(
    String version,
    LocalDateTime startedAt,
    LocalDateTime matchedAt,
    MfdsMatchingReferenceSnapshotItem references,
    List<MfdsAlcoholCandidateItem> alcohols,
    List<MfdsReferenceCandidateItem> distilleries,
    List<MfdsReferenceCandidateItem> regions) {}
