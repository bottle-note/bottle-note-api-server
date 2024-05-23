package app.bottlenote.common.file.upload.dto;

import lombok.Builder;

import java.util.List;

public record ImageUploadResponse(
	String bucketName,
	int uploadSize,
	Integer expiryTime,
	List<ImageUploadInfo> imageUploadInfo
) {
	@Builder
	public ImageUploadResponse {
	}
}
