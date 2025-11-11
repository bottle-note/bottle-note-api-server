package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurationKeywordResponse {
  private Long id;
  private String name;
  private String description;
  private String coverImageUrl;
  private Integer alcoholCount;
  private Integer displayOrder;
}
