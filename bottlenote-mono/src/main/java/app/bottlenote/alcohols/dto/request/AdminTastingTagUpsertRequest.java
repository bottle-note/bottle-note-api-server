package app.bottlenote.alcohols.dto.request;

import app.bottlenote.global.validation.Base64Image;
import jakarta.validation.constraints.NotBlank;

public record AdminTastingTagUpsertRequest(
    @NotBlank(message = "한글 이름은 필수입니다.") String korName,
    @NotBlank(message = "영문 이름은 필수입니다.") String engName,
    @Base64Image String icon,
    String description,
    Long parentId) {}
