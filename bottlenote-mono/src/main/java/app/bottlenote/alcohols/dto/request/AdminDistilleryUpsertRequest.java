package app.bottlenote.alcohols.dto.request;

import app.bottlenote.global.validation.Base64Image;
import jakarta.validation.constraints.NotBlank;

public record AdminDistilleryUpsertRequest(
    @NotBlank(message = "DISTILLERY_KOR_NAME_REQUIRED") String korName,
    @NotBlank(message = "DISTILLERY_ENG_NAME_REQUIRED") String engName,
    @Base64Image String logoImgUrl) {}
