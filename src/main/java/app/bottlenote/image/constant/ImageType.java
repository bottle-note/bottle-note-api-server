package app.bottlenote.image.constant;

//TODO : 기획에 따라 수정 필요
public enum ImageType {
	PNG("image/png", 10 * 1024 * 1024), // 10MB
	JPEG("image/jpeg", 20 * 1024 * 1024), // 20MB
	WEBP("image/webp", 10 * 1024 * 1024), // 10MB
	SVG("image/svg+xml", 5 * 1024 * 1024); // 5MB

	private final String contentType;
	private final long maxSize;

	ImageType(String contentType, long maxSize) {
		this.contentType = contentType;
		this.maxSize = maxSize;
	}

	public String getContentType() {
		return contentType;
	}

	public long getMaxSize() {
		return maxSize;
	}
}
