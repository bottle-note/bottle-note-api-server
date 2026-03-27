package app.bottlenote.banner.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminBannerStatusRequest(
    @NotNull(message = "BANNER_IS_ACTIVE_REQUIRED") Boolean isActive) {}
