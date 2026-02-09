package app.bottlenote.banner.dto.response;

import app.bottlenote.banner.constant.BannerType;
import java.time.LocalDateTime;

public record AdminBannerListResponse(
    Long id,
    String name,
    BannerType bannerType,
    Integer sortOrder,
    Boolean isActive,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime createdAt) {}
