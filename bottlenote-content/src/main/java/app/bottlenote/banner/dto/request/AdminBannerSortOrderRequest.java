package app.bottlenote.banner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminBannerSortOrderRequest(
    @NotNull(message = "BANNER_SORT_ORDER_REQUIRED")
        @Min(value = 0, message = "BANNER_SORT_ORDER_MINIMUM")
        Integer sortOrder) {}
