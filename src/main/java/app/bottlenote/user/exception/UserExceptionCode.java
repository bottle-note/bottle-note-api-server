package app.bottlenote.user.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum UserExceptionCode implements ExceptionCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
	USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
	USER_PASSWORD_NOT_VALID(HttpStatus.BAD_REQUEST, "비밀번호가 유효하지 않습니다."),
	USER_EMAIL_NOT_VALID(HttpStatus.BAD_REQUEST, "이메일이 유효하지 않습니다."),
	USER_NICKNAME_NOT_VALID(HttpStatus.BAD_REQUEST, "닉네임이 유효하지 않습니다."),
	USER_PROFILE_NOT_VALID(HttpStatus.BAD_REQUEST, "프로필이 유효하지 않습니다."),
	USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
	AUTHORIZE_INFO_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 정보를 찾을 수 없습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레쉬 토큰입니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다");


	private final HttpStatus httpStatus;
	private final String message;

	UserExceptionCode(HttpStatus httpStatus, String message) {
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
