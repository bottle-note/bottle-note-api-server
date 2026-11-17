package app.bottlenote.curation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminSpecBasedCurationDetailResponse(
    Long id,
    String name,
    String description,
    String coverImageUrl,
    List<String> imageUrls,
    LocalDate exposureStartDate,
    LocalDate exposureEndDate,
    Integer displayOrder,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt,
    CurationSpecResponse spec,
    Object payload) {}
