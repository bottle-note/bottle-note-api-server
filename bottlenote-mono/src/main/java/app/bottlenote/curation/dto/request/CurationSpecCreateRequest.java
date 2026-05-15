package app.bottlenote.curation.dto.request;

import java.util.Map;

public record CurationSpecCreateRequest(
    String code,
    String name,
    String description,
    Map<String, Object> requestSpec,
    Map<String, Object> responseSpec,
    String hydratorKey,
    Integer version) {}
