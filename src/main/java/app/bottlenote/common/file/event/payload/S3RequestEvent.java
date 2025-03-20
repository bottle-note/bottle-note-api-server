package app.bottlenote.common.file.event.payload;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

public record S3RequestEvent(
    String eventName,
    String bucketName,
    Long requestCount,
    LocalDateTime eventTime
) {
    public S3RequestEvent(String eventName, String bucketName, Long requestCount) {
        this(eventName, bucketName, requestCount, now());
    }

    public static S3RequestEvent of(String eventName, String bucketName, Long requestCount) {
        return new S3RequestEvent(eventName, bucketName, requestCount);
    }
}
