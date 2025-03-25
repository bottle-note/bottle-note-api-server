package app.bottlenote.common.file.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum FileExceptionCode implements ExceptionCode {

	EXPIRY_TIME_RANGE_INVALID(HttpStatus.BAD_REQUEST, "만료 기간의 범위가 적절하지 않습니다.( 최소 1분 ,최대 10분) ");

	private final HttpStatus httpStatus;
	private final String message;

	FileExceptionCode(HttpStatus httpStatus, String message) {
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
