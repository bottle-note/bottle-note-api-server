package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminDistilleryUpsertRequest(
    @NotBlank(message = "DISTILLERY_KOR_NAME_REQUIRED") String korName,
    @NotBlank(message = "DISTILLERY_ENG_NAME_REQUIRED") String engName,
    String imageUrl) {}
