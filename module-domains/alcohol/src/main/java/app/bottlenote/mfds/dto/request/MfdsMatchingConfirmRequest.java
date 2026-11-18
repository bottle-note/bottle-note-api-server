package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotNull;

/** 매칭 확정 요청. 후보에 없는 ID도 허용한다(수동 매칭). distillery/region은 선택 사항이다. */
public record MfdsMatchingConfirmRequest(
    @NotNull(message = "alcoholId는 필수입니다.") Long alcoholId, Long distilleryId, Long regionId) {}
