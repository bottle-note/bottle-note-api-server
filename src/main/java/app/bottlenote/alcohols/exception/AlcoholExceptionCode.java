package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum AlcoholExceptionCode implements ExceptionCode {

	ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String message;

	AlcoholExceptionCode(HttpStatus httpStatus, String message) {
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
