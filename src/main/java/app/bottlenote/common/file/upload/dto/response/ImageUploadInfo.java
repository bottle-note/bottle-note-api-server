package app.bottlenote.common.file.upload.dto.response;

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
