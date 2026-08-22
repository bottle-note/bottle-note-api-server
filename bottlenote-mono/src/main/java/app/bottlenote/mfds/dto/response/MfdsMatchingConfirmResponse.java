package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;

/** 매칭 확정·해제 처리 결과. */
public record MfdsMatchingConfirmResponse(
    Long declarationId,
    Long selectedAlcoholId,
    MfdsMatchSelectionSource alcoholMatchDecision,
    Long selectedDistilleryId,
    MfdsMatchSelectionSource distilleryMatchSource,
    Long selectedRegionId,
    MfdsMatchSelectionSource regionMatchSource) {}
