package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import java.util.List;

public record AdminAlcoholMergePreviewResponse(AlcoholItem target, List<SourceItem> sources) {

  public record AlcoholItem(Long alcoholId, String korName, String engName, String imageUrl) {}

  public record SourceItem(AlcoholItem alcohol, List<AlcoholMergeCount> counts) {}
}
