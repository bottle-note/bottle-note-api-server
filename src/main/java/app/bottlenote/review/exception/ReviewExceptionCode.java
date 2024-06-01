package app.bottlenote.review.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ReviewExceptionCode implements ExceptionCode {

	INVALID_TASTING_TAG_LENGTH(HttpStatus.BAD_REQUEST, "테이스팅 태그의 길이는 12자 이하로만 작성할 수 있습니다."),
	INVALID_TASTING_TAG_LIST_SIZE(HttpStatus.BAD_REQUEST, "테이스팅 태그는 10개까지만 작성할 수 있습니다."),
	INVALID_CALL_BACK_URL(HttpStatus.BAD_REQUEST, "잘못된 콜백 URL입니다.");
	private final HttpStatus httpStatus;
	private final String message;

	ReviewExceptionCode(HttpStatus httpStatus, String message) {
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
