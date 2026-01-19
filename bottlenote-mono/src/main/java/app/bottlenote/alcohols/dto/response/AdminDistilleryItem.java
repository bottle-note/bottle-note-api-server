package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminDistilleryItem(
    Long id,
    String korName,
    String engName,
    String logoImgUrl,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
