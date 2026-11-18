package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminRegionItem(
    Long id,
    String korName,
    String engName,
    String continent,
    String description,
    String imageUrl,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt,
    Long parentId,
    Integer sortOrder) {}
