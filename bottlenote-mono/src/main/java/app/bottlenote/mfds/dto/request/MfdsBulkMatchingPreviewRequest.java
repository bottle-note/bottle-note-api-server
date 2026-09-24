package app.bottlenote.mfds.dto.request;

import jakarta.validation.constraints.NotNull;

/** 같은 제품 일괄 적용 미리보기. 증류소·지역을 생략하면 주류에 등록된 값을 사용한다. */
public record MfdsBulkMatchingPreviewRequest(
    @NotNull(message = "sourceDeclarationId는 필수입니다.") Long sourceDeclarationId,
    @NotNull(message = "alcoholId는 필수입니다.") Long alcoholId,
    Long distilleryId,
    Long regionId) {}
