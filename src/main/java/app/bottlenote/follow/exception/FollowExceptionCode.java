package app.bottlenote.follow.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum FollowExceptionCode implements ExceptionCode {

	CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우, 언팔로우 할 수 없습니다."),
	FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우할 대상을 찾을 수 없습니다."),
	STATUS_NOT_FOUND (HttpStatus.BAD_REQUEST,"FOLLOWING, UNFOLLOW 중 하나를 입력해주세요.");

	private final HttpStatus httpStatus;
	private final String message;

	FollowExceptionCode(HttpStatus httpStatus, String message) {
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
