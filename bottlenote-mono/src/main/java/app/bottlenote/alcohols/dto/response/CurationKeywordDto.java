package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurationKeywordDto {
  private Long id;
  private String name;
  private String description;
  private Integer alcoholCount;
  private Integer displayOrder;
}
