package app.bottlenote.mfds.dto.response;

import java.util.List;

/** 선택한 신고를 한 번에 확정한 결과. 일부만 반영하지 않는다. */
public record MfdsBulkMatchingConfirmResponse(
    Long sourceDeclarationId,
    Long alcoholId,
    String alcoholNameKo,
    String alcoholNameEn,
    Long distilleryId,
    Long regionId,
    int appliedCount,
    int unchangedCount,
    List<MfdsBulkMatchingConfirmItem> items) {}
