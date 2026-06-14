package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class RegionsItem {
  private final Long regionId;
  private final String korName;
  private final String engName;
  private final String description;
  private final String imageUrl;
  private final Long parentId;
  private final Integer sortOrder;

  public static RegionsItem of(
      Long regionId,
      String korName,
      String engName,
      String description,
      Long parentId,
      Integer sortOrder) {
    return of(regionId, korName, engName, description, null, parentId, sortOrder);
  }
}
