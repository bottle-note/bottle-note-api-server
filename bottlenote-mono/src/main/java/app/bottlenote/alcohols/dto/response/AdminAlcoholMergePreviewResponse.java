package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import java.util.List;

public record AdminAlcoholMergePreviewResponse(AlcoholItem base, List<DuplicateItem> duplicates) {

  public record AlcoholItem(Long alcoholId, String korName, String engName, String imageUrl) {}

  public record DuplicateItem(AlcoholItem alcohol, List<AlcoholMergeCount> counts) {}
}
