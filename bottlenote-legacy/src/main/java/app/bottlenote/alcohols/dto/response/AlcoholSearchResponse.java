package app.bottlenote.alcohols.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class AlcoholSearchResponse {
  private Long totalCount;
  private List<AlcoholsSearchItem> alcohols;
}
