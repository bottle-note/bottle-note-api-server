package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/** 여러 위스키에 같은 속성을 추가하는 요청. 항목·값 형식은 도메인이 검증한다. */
public record AdminAlcoholAttributeCreateRequest(
    @NotEmpty(message = "ALCOHOL_ID_REQUIRED")
        List<@NotNull(message = "ALCOHOL_ID_REQUIRED") Long> alcoholIds,
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_KEY_REQUIRED") String key,
    @NotNull(message = "ALCOHOL_ATTRIBUTE_TYPE_REQUIRED") AttributeType type,
    @NotNull(message = "ALCOHOL_ATTRIBUTE_DATATYPE_REQUIRED") AttributeDatatype datatype,
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_VALUE_REQUIRED") String value,
    String description,
    LocalDateTime basedAt) {}
