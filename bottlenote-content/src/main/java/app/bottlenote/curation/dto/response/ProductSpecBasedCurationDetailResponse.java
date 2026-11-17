package app.bottlenote.curation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProductSpecBasedCurationDetailResponse(
    Long id,
    String name,
    String description,
    String coverImageUrl,
    List<String> imageUrls,
    LocalDate exposureStartDate,
    LocalDate exposureEndDate,
    Integer displayOrder,
    LocalDateTime createAt,
    SpecMeta spec,
    Object payload) {

  public record SpecMeta(
      Long id, String code, String name, String container, Map<String, Object> responseSpec) {}
}
