package app.bottlenote.common.file.upload.dto.response;

import lombok.Builder;

@Builder
public record ImageUploadInfo(
	Long order,
	String viewUrl,
	String uploadUrl
) {
}
