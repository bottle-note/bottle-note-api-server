package app.bottlenote.mfds.dto.response;

import java.util.List;

/** 일괄 확정 결과. 이미 같은 값이 연결된 신고는 쓰지 않고 unchangedDeclarationIds로 돌려준다. */
public record MfdsBulkMatchingConfirmResponse(
    List<MfdsMatchingConfirmResponse> applied, List<Long> unchangedDeclarationIds) {}
