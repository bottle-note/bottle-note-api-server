package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;
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
    Double popularScore,
    LocalDateTime viewAt) {}
