package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** 속성 한 건의 분류·형식·값·근거·기준 시점을 수정하는 요청. 항목(key)은 바꾸지 않는다. */
public record AdminAlcoholAttributeUpdateRequest(
    @NotNull(message = "ALCOHOL_ATTRIBUTE_TYPE_REQUIRED") AttributeType type,
    @NotNull(message = "ALCOHOL_ATTRIBUTE_DATATYPE_REQUIRED") AttributeDatatype datatype,
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_VALUE_REQUIRED") String value,
    String description,
    LocalDateTime basedAt) {}
