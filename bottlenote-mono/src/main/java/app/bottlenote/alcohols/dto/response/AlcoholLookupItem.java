package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import java.util.Locale;
import java.util.stream.Stream;

public record AlcoholLookupItem(
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
    String imageUrl) {

  public String searchText() {
    return Stream.of(
            korName,
            engName,
            korCategoryName,
            engCategoryName,
            categoryGroup != null ? categoryGroup.name() : null,
            korRegion,
            engRegion,
            korDistillery,
            engDistillery)
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .reduce("", (left, right) -> left + " " + right);
  }
}
