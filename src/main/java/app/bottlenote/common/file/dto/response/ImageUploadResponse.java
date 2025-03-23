package app.bottlenote.common.file.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ImageUploadResponse(
	String bucketName,
	int uploadSize,
	Integer expiryTime,
	List<ImageUploadItem> imageUploadItem
) {
}
