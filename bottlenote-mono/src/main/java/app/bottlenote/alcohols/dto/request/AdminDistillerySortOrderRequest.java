package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminDistillerySortOrderRequest(
    @NotNull(message = "DISTILLERY_SORT_ORDER_REQUIRED")
        @Min(value = 0, message = "DISTILLERY_SORT_ORDER_MINIMUM")
        Integer sortOrder) {}
