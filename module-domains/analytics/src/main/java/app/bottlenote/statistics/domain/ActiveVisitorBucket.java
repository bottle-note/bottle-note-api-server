package app.bottlenote.statistics.domain;

import java.time.LocalDateTime;

public record ActiveVisitorBucket(LocalDateTime bucketAt, long visitors, long members) {}
