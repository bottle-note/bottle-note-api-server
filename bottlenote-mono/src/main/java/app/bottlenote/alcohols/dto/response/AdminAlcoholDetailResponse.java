package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAlcoholDetailResponse(
    Long alcoholId,
    String korName,
    String engName,
    String imageUrl,
    String type,
    String korCategory,
    String engCategory,
    String categoryGroup,
    String abv,
    String age,
    String cask,
    String volume,
    String description,
    Long regionId,
    String korRegion,
    String engRegion,
    Long distilleryId,
    String korDistillery,
    String engDistillery,
    List<TastingTagInfo> tastingTags,
    Double avgRating,
    Long totalRatingsCount,
    Long reviewCount,
    Long pickCount,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {

  public record TastingTagInfo(Long id, String korName, String engName) {}
}
