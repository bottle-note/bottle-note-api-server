package app.bottlenote.campaigncontent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 코드는 FE 배포물에 박혀 있어 수정 대상에서 뺀다. */
public record AdminCampaignContentUpdateRequest(
    @NotBlank(message = "이름은 필수입니다.") @Size(max = 100, message = "이름은 100자 이하여야 합니다.") String name,
    @Size(max = 255, message = "설명은 255자 이하여야 합니다.") String description,
    @NotNull(message = "활성 여부는 필수입니다.") Boolean isActive) {}
