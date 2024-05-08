package app.bottlenote.image.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ImageExceptionCode implements ExceptionCode {

	FAILED_SAVE_IMAGE(HttpStatus.BAD_REQUEST, "이미지 저장에 실패했습니다."),
	INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST,
		"허용되지 않은 확장자의 파일입니다. (PNG, JPEG, WEBP, SVG 형식의 파일만 업로드 가능합니다)");


	private final HttpStatus httpStatus;
	private final String message;

	ImageExceptionCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
