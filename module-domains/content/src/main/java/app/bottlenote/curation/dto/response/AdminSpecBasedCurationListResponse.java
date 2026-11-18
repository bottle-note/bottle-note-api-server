package app.bottlenote.curation.dto.response;

import java.time.LocalDateTime;

public record AdminSpecBasedCurationListResponse(
    Long id,
    Long specId,
    String specCode,
    String name,
    Integer displayOrder,
    Boolean isActive,
    LocalDateTime createdAt) {}
