package app.bottlenote.mfds.dto.response;

import java.util.List;

/** 선택한 신고를 한 번에 확정한 결과. 일부만 반영하지 않는다. */
public record MfdsBulkMatchingConfirmResponse(
    List<MfdsMatchingConfirmResponse> applied, List<Long> unchangedDeclarationIds) {}
