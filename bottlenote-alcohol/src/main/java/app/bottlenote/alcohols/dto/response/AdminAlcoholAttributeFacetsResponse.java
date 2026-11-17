package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholAttributeFacetsResponse(
    List<AdminAlcoholAttributeFacetItem> items, boolean hasNext) {
  public record AdminAlcoholAttributeFacetItem(
      AdminAlcoholAttributeItem attribute, String value, long alcoholCount) {}
}
