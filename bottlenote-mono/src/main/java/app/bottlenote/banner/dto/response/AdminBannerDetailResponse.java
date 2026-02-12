package app.bottlenote.banner.dto.response;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
import java.time.LocalDateTime;

public record AdminBannerDetailResponse(
    Long id,
    String name,
    String nameFontColor,
    String descriptionA,
    String descriptionB,
    String descriptionFontColor,
    String imageUrl,
    TextPosition textPosition,
    Boolean isExternalUrl,
    String targetUrl,
    BannerType bannerType,
    Integer sortOrder,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
