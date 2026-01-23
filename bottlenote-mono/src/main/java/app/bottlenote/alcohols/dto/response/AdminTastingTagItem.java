package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminTastingTagItem(
    Long id,
    String korName,
    String engName,
    String icon,
    String description,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
