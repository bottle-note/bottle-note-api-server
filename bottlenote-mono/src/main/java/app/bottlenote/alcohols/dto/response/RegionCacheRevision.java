package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record RegionCacheRevision(long count, LocalDateTime lastModifyAt) {}
