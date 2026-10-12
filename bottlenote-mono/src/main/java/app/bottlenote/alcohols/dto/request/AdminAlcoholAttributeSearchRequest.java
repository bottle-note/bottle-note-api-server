package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 선택한 위스키들의 속성을 조회할 때 쓰는 요청. */
public record AdminAlcoholAttributeSearchRequest(
    @NotEmpty(message = "ALCOHOL_ID_REQUIRED")
        List<@NotNull(message = "ALCOHOL_ID_REQUIRED") Long> alcoholIds) {}
