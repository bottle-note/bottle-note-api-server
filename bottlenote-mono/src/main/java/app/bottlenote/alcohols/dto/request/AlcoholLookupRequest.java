package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

public record AlcoholLookupRequest(
    String keyword,
    String category,
    Long regionId,
    Long distilleryId,
    @Min(0) Long cursor,
    @Min(1) @Max(100) Long pageSize) {
  @Builder
  public AlcoholLookupRequest {
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 20L;
  }

  public AlcoholCategoryGroup categoryGroup() {
    if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
      return null;
    }
    return AlcoholCategoryGroup.fromCategory(category);
  }
}
