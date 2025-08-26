package app.bottlenote.alcohols.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class PopularsOfWeekResponse {
  private final Integer totalCount;
  private final List<PopularItem> alcohols;
}
