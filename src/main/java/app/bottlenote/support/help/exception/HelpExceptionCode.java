package app.bottlenote.support.help.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum HelpExceptionCode implements ExceptionCode {

	HELP_NOT_FOUND(HttpStatus.BAD_REQUEST, "문의글을 찾을 수 없습니다"),
	HELP_NOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "문의글 수정/삭제 권한이 없습니다");

	private final HttpStatus httpStatus;
	private final String message;

	HelpExceptionCode(HttpStatus httpStatus, String message) {
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
