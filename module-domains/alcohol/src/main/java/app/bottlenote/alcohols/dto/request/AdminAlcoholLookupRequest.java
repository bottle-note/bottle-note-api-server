package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import lombok.Builder;

/**
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기
 */
public record AdminAlcoholLookupRequest(
    String keyword, String category, Long regionId, Long distilleryId, Integer page, Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AdminAlcoholLookupRequest {
    page = page != null && page >= 0 ? page : 0;
    if (size == null || size < 1) {
      size = DEFAULT_SIZE;
    } else {
      size = Math.min(size, MAX_SIZE);
    }
  }

  public AlcoholCategoryGroup categoryGroup() {
    if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
      return null;
    }
    return AlcoholCategoryGroup.fromCategory(category);
  }
}
