package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminRegionItem(
    Long id,
    String korName,
    String engName,
    String continent,
    String description,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
