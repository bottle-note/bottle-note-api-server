package app.bottlenote.follow.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum FollowExceptionCode implements ExceptionCode {

	CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우, 언팔로우 할 수 없습니다."),
	ALREADY_FOLLOWING(HttpStatus.CONFLICT, "이미 팔로우한 사용자입니다."),
	ALREADY_UNFOLLOWING(HttpStatus.CONFLICT, "이미 언팔로우한 사용자입니다."),
	FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우할 대상을 찾을 수 없습니다."),
	CANNOT_UNFOLLOW(HttpStatus.BAD_REQUEST, "팔로우 하지않아 언팔로우 할 수 없습니다."),
	STATUS_NOT_FOUND (HttpStatus.BAD_REQUEST,"상태가 없습니다 : FOLLOWING, UNFOLLOW, BLOCK, HIDDEN");

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
