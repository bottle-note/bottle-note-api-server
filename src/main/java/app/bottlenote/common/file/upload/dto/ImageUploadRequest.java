package app.bottlenote.common.file.upload.dto;

public record ImageUploadRequest(
	String rootPath,
	Long uploadSize
) {
	public ImageUploadRequest {
		uploadSize = uploadSize == null ? 1 : uploadSize;
	}
}
