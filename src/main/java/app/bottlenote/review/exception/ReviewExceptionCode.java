package app.bottlenote.review.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ReviewExceptionCode implements ExceptionCode {

	REVIEW_NOT_FOUND(HttpStatus.BAD_REQUEST, "리뷰를 찾을 수 없습니다"),
	INVALID_TASTING_TAG_LENGTH(HttpStatus.BAD_REQUEST, "테이스팅 태그의 길이는 12자 이하로만 작성할 수 있습니다."),
	INVALID_TASTING_TAG_LIST_SIZE(HttpStatus.BAD_REQUEST, "테이스팅 태그는 10개까지만 작성할 수 있습니다."),
	INVALID_IMAGE_URL_MAX_SIZE(HttpStatus.BAD_REQUEST, "이미지는 최대 5장까지만 업로드 할 수 있습니다"),
	INVALID_CALL_BACK_URL(HttpStatus.BAD_REQUEST, "잘못된 콜백 URL입니다."),
	NOT_FOUND_REVIEW_REPLY(HttpStatus.BAD_REQUEST, "댓글을 찾을 수 없습니다."),
	REPLY_NOT_OWNER(HttpStatus.BAD_REQUEST, "댓글 작성자만 삭제할 수 있습니다.");

	private final HttpStatus httpStatus;
	private final String message;

	ReviewExceptionCode(HttpStatus httpStatus, String message) {
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
