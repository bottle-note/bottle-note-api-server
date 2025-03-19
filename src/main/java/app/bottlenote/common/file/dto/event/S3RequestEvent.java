package app.bottlenote.common.file.dto.event;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@ToString(of = {"eventName", "bucketName", "requestCount", "eventTime"})
@Getter
public class S3RequestEvent {
	private final String eventName;
	private final String bucketName;
	private final Long requestCount;
	private final LocalDateTime eventTime;

	private S3RequestEvent(String eventName, String bucketName, Long requestCount) {
		this.eventName = eventName;
		this.bucketName = bucketName;
		this.requestCount = requestCount;
		this.eventTime = now();
	}

	public static S3RequestEvent of(String eventName, String bucketName, Long requestCount) {
		return new S3RequestEvent(eventName, bucketName, requestCount);
	}
}
