package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAlcoholAttributeUpdateRequest(
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_NAME_REQUIRED")
        @Size(max = 100, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
        String displayName,
    @NotNull(message = "ALCOHOL_ATTRIBUTE_TYPE_REQUIRED") AttributeType type,
    @NotNull(message = "ALCOHOL_ATTRIBUTE_DATATYPE_REQUIRED") AttributeDatatype datatype,
    @Size(max = 20, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID") String unit,
    @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID") String description) {}
