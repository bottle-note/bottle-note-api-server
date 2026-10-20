package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.*;

public record AdminAlcoholAttributeGroupRequest(
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
        @Size(max = 100, message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
        String name,
    @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE") String description,
    @PositiveOrZero(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE") Integer displayOrder) {}
