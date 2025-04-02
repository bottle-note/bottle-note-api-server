package app.bottlenote.user.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum UserExceptionCode implements ExceptionCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
	REQUIRED_USER_ID(HttpStatus.BAD_REQUEST, "유저 아이디가 필요합니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	USER_DELETED(HttpStatus.BAD_REQUEST, "탈퇴한 유저입니다."),
	USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
	USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
	USER_PASSWORD_NOT_VALID(HttpStatus.BAD_REQUEST, "비밀번호가 유효하지 않습니다."),
	USER_EMAIL_NOT_VALID(HttpStatus.BAD_REQUEST, "이메일이 유효하지 않습니다."),
	EMAIL_IS_REQUIRED(HttpStatus.BAD_REQUEST, "이메일은 필수 입력 값입니다."),
	PASSWORD_IS_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호는 필수 입력 값입니다."),
	AGE_NEED_OVER_19(HttpStatus.BAD_REQUEST, "나이는 19세 이상이어야 합니다."),
	USER_PROFILE_NOT_VALID(HttpStatus.BAD_REQUEST, "프로필이 유효하지 않습니다."),
	USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
	AUTHORIZE_INFO_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 정보를 찾을 수 없습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레쉬 토큰입니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
	INVALID_CALL_BACK_URL(HttpStatus.BAD_REQUEST, "잘못된 콜백 URL입니다."),
	NOTIFICATION_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "알림 대상 유저를 찾을 수 없습니다."),
	MYPAGE_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "접근할 수 없는 마이페이지입니다."),
	MYBOTTLE_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "접근할 수 없는 마이보틀페이지입니다."),
	USER_NICKNAME_NOT_VALID(HttpStatus.BAD_REQUEST, "중복된 닉네임입니다."),
	JSON_PARSING_EXCEPTION(HttpStatus.BAD_REQUEST, "JSON 처리 중 오류가 발생했습니다"),
	NOT_MATCH_GUEST_CODE(HttpStatus.BAD_REQUEST, "게스트 코드가 일치하지 않습니다."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
	TEMPORARY_LOGIN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 로그인 오류입니다.");
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
