package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminRegionDetailResponse(
    Long id,
    String korName,
    String engName,
    String continent,
    String description,
    String imageUrl,
    Integer sortOrder,
    Long parentId,
    String parentKorName,
    boolean hasChildren,
    long alcoholCount,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt) {}
