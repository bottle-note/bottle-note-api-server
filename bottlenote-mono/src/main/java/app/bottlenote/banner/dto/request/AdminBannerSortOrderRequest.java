package app.bottlenote.banner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminBannerSortOrderRequest(
    @NotNull(message = "정렬 순서는 필수입니다.") @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
        Integer sortOrder) {}
