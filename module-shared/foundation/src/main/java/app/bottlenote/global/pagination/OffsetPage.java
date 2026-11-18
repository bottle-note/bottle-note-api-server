package app.bottlenote.global.pagination;

import java.util.List;

public record OffsetPage<T>(List<T> content, long totalElements) {}
