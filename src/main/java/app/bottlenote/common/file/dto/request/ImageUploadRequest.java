package app.bottlenote.common.file.dto.request;

public record ImageUploadRequest(
	String rootPath,
	Long uploadSize
) {
	public ImageUploadRequest {
		uploadSize = uploadSize == null ? 1 : uploadSize;
	}
}
