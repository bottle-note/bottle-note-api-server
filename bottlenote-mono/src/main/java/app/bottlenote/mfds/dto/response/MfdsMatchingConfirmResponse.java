package app.bottlenote.mfds.dto.response;

/** 매칭 확정·해제 처리 결과. */
public record MfdsMatchingConfirmResponse(
    Long declarationId,
    Long selectedAlcoholId,
    String alcoholMatchDecision,
    Long selectedDistilleryId,
    String distilleryMatchSource,
    Long selectedRegionId,
    String regionMatchSource) {}
