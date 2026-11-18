package app.bottlenote.curation.dto.response;

public record CurationSpecListResponse(
    Long id, String code, String name, String description, Integer version, Boolean isActive) {}
