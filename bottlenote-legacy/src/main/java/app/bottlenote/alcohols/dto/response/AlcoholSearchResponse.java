package app.bottlenote.alcohols.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class AlcoholSearchResponse {
  private final Long totalCount;
  private final List<AlcoholsSearchItem> alcohols;
}
