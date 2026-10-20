package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholAttributeFacets(
    List<AdminAlcoholAttributeFacetItem> items, boolean hasNext) {
  public record AdminAlcoholAttributeFacetItem(
      AdminAlcoholAttributeItem attribute, String value, long alcoholCount) {}
}
