package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import java.util.Locale;
import java.util.stream.Stream;

public record AlcoholLookupSnapshotItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategoryName,
    String engCategoryName,
    AlcoholCategoryGroup categoryGroup,
    Long regionId,
    String korRegion,
    String engRegion,
    Long distilleryId,
    String korDistillery,
    String engDistillery,
    String imageUrl,
    String normalizedSearchText) {

  public static AlcoholLookupSnapshotItem from(AlcoholLookupItem item) {
    return new AlcoholLookupSnapshotItem(
        item.alcoholId(),
        item.korName(),
        item.engName(),
        item.korCategoryName(),
        item.engCategoryName(),
        item.categoryGroup(),
        item.regionId(),
        item.korRegion(),
        item.engRegion(),
        item.distilleryId(),
        item.korDistillery(),
        item.engDistillery(),
        item.imageUrl(),
        normalize(
            item.korName(),
            item.engName(),
            item.korCategoryName(),
            item.engCategoryName(),
            item.categoryGroup() != null ? item.categoryGroup().name() : null,
            item.korRegion(),
            item.engRegion(),
            item.korDistillery(),
            item.engDistillery()));
  }

  public AlcoholLookupItem toLookupItem() {
    return new AlcoholLookupItem(
        alcoholId,
        korName,
        engName,
        korCategoryName,
        engCategoryName,
        categoryGroup,
        regionId,
        korRegion,
        engRegion,
        distilleryId,
        korDistillery,
        engDistillery,
        imageUrl);
  }

  public String normalizedSearchText() {
    if (normalizedSearchText != null && !normalizedSearchText.isBlank()) {
      return normalizedSearchText;
    }
    return normalize(
        korName,
        engName,
        korCategoryName,
        engCategoryName,
        categoryGroup != null ? categoryGroup.name() : null,
        korRegion,
        engRegion,
        korDistillery,
        engDistillery);
  }

  private static String normalize(String... values) {
    return Stream.of(values)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .reduce("", (left, right) -> left + " " + right);
  }
}
