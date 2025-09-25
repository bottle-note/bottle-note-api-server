package app.bottlenote.shared.alcohols.dto.response;

import lombok.Builder;

@Builder
public record ViewHistoryItem(
    Long alcoholId,
    String korName,
    String engName,
    Double rating,
    Long ratingCount,
    String korCategory,
    String engCategory,
    String imageUrl,
    Boolean isPicked,
    Double popularScore) {}
