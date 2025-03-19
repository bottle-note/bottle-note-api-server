package app.bottlenote.common.file.dto.response;

import lombok.Builder;

@Builder
public record ImageUploadInfo(
	Long order,
	String viewUrl,
	String uploadUrl
) {
}
