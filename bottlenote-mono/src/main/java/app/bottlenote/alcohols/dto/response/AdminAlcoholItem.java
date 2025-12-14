package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminAlcoholItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategoryName,
    String engCategoryName,
    String imageUrl,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
