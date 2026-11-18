package app.bottlenote.curation.dto.response;

public record CurationFeedFieldResponse(
    String path, String role, Integer order, String description, Object value) {}
