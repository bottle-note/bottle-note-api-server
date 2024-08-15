package app.bottlenote.global.exception.custom.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ValidExceptionCode implements ExceptionCode {

	//COMMON
	VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "필드의 값이 NULL일 수 없습니다"),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "잘못된 타입입니다."),
	JSON_PASSING_FAILED(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패했습니다."),
	UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 추가적인 문의가 필요합니다."),
	JWT_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "JWT 토큰 관련 예외가 발생했습니다."),
	AWS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AWS 관련 오류가 발생했습니다. infra 팀에 문의해주세요."),


	//ALCOHOL
	ALCOHOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "알코올 식별자는 필수입니다."),
	ALCOHOL_ID_MINIMUM(HttpStatus.BAD_REQUEST, "알코올 식별자는 최소 1 이상 이어야 합니다."),

	//PICK
	PICK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "픽 식별자는 필수입니다."),
	IS_PICKED_REQUIRED(HttpStatus.BAD_REQUEST, "픽 여부는 필수입니다."),

	//REVIEW
	REVIEW_ID_MINIMUM(HttpStatus.BAD_REQUEST, "리뷰 식별자는 최소 1 이상이어야 합니다."),
	REVIEW_ID_REQUIRED(HttpStatus.BAD_REQUEST, "리뷰 식별자는 필수입니다."),
	REVIEW_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "리뷰 내용은 필수입니다."),
	REVIEW_CONTENT_MAXIMUM(HttpStatus.BAD_REQUEST, "리뷰 내용의 최대 글자수를 초과했습니다."),
	PRICE_MINIMUM(HttpStatus.BAD_REQUEST, "가격은 0원 이상이어야 합니다."),
	PRICE_MAXIMUM(HttpStatus.BAD_REQUEST, "입력할 수 있는 가격의 범위가 아닙니다."),

	//LOCATION INFO
	ZIPCODE_ONLY_NUMBER(HttpStatus.BAD_REQUEST, "우편번호는 숫자만 가능합니다"),
	ZIPCODE_FORMAT(HttpStatus.BAD_REQUEST, "우편번호는 5자리의 숫자만 가능합니다");


	private final HttpStatus httpStatus;
	private String message;

	public ValidExceptionCode message(String errorMessage) {
		this.message = errorMessage;
		return this;
	}

	public void appendMessage(String s) {
		this.message += s;
	}
}
