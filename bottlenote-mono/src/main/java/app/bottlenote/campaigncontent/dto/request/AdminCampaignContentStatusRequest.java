package app.bottlenote.campaigncontent.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminCampaignContentStatusRequest(
    @NotNull(message = "활성 여부는 필수입니다.") Boolean isActive) {}
