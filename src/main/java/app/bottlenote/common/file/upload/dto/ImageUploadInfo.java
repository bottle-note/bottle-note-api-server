package app.bottlenote.common.file.upload.dto;

import lombok.Builder;

public record ImageUploadInfo(
	Long order,
	String viewUrl,
	String uploadUrl
) {
	@Builder
	public ImageUploadInfo {
	}
}
