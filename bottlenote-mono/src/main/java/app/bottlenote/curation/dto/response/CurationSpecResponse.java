package app.bottlenote.curation.dto.response;

import java.util.Map;

public record CurationSpecResponse(
    Long id,
    String code,
    String name,
    String description,
    String hydratorKey,
    Integer version,
    Boolean isActive,
    Map<String, Object> requestSpec,
    Map<String, Object> responseSpec) {}
