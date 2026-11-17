package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin 큐레이션 노출 순서 변경 요청
 *
 * @param displayOrder 노출 순서 (0 이상)
 */
public record AdminCurationDisplayOrderRequest(
    @NotNull(message = "노출 순서는 필수입니다.") @Min(value = 0, message = "노출 순서는 0 이상이어야 합니다.")
        Integer displayOrder) {}
