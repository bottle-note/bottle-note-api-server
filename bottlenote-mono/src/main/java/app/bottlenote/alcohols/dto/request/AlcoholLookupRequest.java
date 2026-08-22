package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.global.pagination.KeysetPageRequest;
import lombok.Builder;

public record AlcoholLookupRequest(
    String keyword,
    String category,
    Long regionId,
    Long distilleryId,
    String cursor,
    Integer size) {
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AlcoholLookupRequest {
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }

  public AlcoholCategoryGroup categoryGroup() {
    if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
      return null;
    }
    return AlcoholCategoryGroup.fromCategory(category);
  }
}
