package app.bottlenote.common.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum CommonExceptionCode implements ExceptionCode {

	CONTAINS_PROFANITY(HttpStatus.BAD_REQUEST, "비속어가 포함되어 있습니다.");

	private final HttpStatus httpStatus;
	private final String message;

	CommonExceptionCode(HttpStatus httpStatus, String message) {
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
