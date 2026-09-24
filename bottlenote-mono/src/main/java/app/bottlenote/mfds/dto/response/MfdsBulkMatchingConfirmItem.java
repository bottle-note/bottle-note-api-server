package app.bottlenote.mfds.dto.response;

/** 일괄 확정 결과의 신고 한 건. outcome은 APPLIED 또는 UNCHANGED다. */
public record MfdsBulkMatchingConfirmItem(
    Long declarationId,
    String outcome,
    Long selectedAlcoholId,
    String alcoholMatchDecision,
    Long selectedDistilleryId,
    String distilleryMatchSource,
    Long selectedRegionId,
    String regionMatchSource) {}
