package app.bottlenote.history.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum UserHistoryExceptionCode implements ExceptionCode {

	INVALID_HISTORY_DATE(HttpStatus.BAD_REQUEST, "부적절한 검색 조건 기간입니다."),
	INVALID_HISTORY_DATE_RANGE(HttpStatus.BAD_REQUEST, "검색 조건 기간은 최대 2년까지 가능합니다.");

	private final HttpStatus httpStatus;
	private final String message;

	UserHistoryExceptionCode(HttpStatus httpStatus, String message) {
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
