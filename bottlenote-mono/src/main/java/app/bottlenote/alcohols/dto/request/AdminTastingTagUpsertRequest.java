package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminTastingTagUpsertRequest(
    @NotBlank(message = "한글 이름은 필수입니다.") String korName,
    @NotBlank(message = "영문 이름은 필수입니다.") String engName,
    String icon,
    String description,
    Long parentId) {}
