package app.bottlenote.statistics.domain;

import java.time.LocalDateTime;

public record ReturningVisitorBucket(
    LocalDateTime bucketAt, long visitors, long returningVisitors) {}
