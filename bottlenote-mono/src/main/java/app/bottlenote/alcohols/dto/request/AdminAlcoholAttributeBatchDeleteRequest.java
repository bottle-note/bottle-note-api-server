package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 속성 여러 건을 한 번에 지우는 요청. */
public record AdminAlcoholAttributeBatchDeleteRequest(
    @NotEmpty(message = "ALCOHOL_ATTRIBUTE_IDS_REQUIRED")
        List<@NotNull(message = "ALCOHOL_ATTRIBUTE_ID_REQUIRED") Long> ids) {}
