package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminTastingTagAlcoholRequest(
    @NotEmpty(message = "위스키 ID 목록은 필수입니다.") List<Long> alcoholIds) {}
