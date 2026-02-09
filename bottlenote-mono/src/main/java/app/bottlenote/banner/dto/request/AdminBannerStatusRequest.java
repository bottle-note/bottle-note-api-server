package app.bottlenote.banner.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminBannerStatusRequest(@NotNull(message = "활성화 상태는 필수입니다.") Boolean isActive) {}
