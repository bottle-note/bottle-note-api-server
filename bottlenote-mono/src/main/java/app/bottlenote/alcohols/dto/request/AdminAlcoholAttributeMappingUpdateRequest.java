package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAlcoholAttributeMappingUpdateRequest(
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_VALUE_REQUIRED")
        @Size(max = 255, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
        String value) {}
