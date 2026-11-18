package app.bottlenote.curation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductSpecBasedCurationListResponse(
    Long id,
    Long specId,
    String specCode,
    String specName,
    String name,
    String description,
    String coverImageUrl,
    List<String> imageUrls,
    LocalDate exposureStartDate,
    LocalDate exposureEndDate,
    Integer displayOrder,
    LocalDateTime createAt) {}
