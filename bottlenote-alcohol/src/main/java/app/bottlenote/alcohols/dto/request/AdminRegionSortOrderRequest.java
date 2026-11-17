package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminRegionSortOrderRequest(
    @NotNull(message = "REGION_SORT_ORDER_REQUIRED")
        @Min(value = 0, message = "REGION_SORT_ORDER_MINIMUM")
        Integer sortOrder) {}
