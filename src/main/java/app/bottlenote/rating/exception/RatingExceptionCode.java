package app.bottlenote.rating.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum RatingExceptionCode implements ExceptionCode {
	INVALID_RATING_POINT(HttpStatus.BAD_REQUEST, "평점은 0.0/1.0/1.5/2.0/2.5/3.0/3.5/4.0/4.5/5.0 중 하나의 값을 가질 수 있습니다."),
	INPUT_NUMBER_IS_NOT_A_NUMBER(HttpStatus.BAD_REQUEST, "입력된 값이 숫자가 아닙니다: "),
	INPUT_VALUE_IS_NOT_VALID(HttpStatus.BAD_REQUEST, "유효하지 않은 별점 값");

	private final HttpStatus httpStatus;
	private final String message;

	RatingExceptionCode(HttpStatus httpStatus, String message) {
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
