package app.bottlenote.global.exception.custom.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ValidExceptionCode implements ExceptionCode {

	//COMMON
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "잘못된 타입입니다."),
	JSON_PASSING_FAILED(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패했습니다."),
	UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 추가적인 문의가 필요합니다."),
	JWT_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "JWT 토큰 관련 예외가 발생했습니다."),
	AWS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AWS 관련 오류가 발생했습니다. infra 팀에 문의해주세요."),
	CONTENT_NOT_BLANK(HttpStatus.BAD_REQUEST, "공백입니다. 내용을 입력해주세요."),
	CONTENT_NOT_EMPTY(HttpStatus.BAD_REQUEST, "null입니다. 내용을 입력해주세요."),
	TITLE_NOT_EMPTY(HttpStatus.BAD_REQUEST, "null입니다. 타이틀(제목)을 입력해주세요."),


	//EXTERNAL
	DEVICE_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "디바이스 토큰은 필수입니다."),
	PLATFORM_REQUIRED(HttpStatus.BAD_REQUEST, "플랫폼은 필수입니다."),


	//ALCOHOL
	ALCOHOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "알코올 식별자는 필수입니다."),
	ALCOHOL_ID_MINIMUM(HttpStatus.BAD_REQUEST, "알코올 식별자는 최소 1 이상 이어야 합니다."),

	//PICK,
	PICK_ID_REQUIRED(HttpStatus.BAD_REQUEST, "픽 식별자는 필수입니다."),
	IS_PICKED_REQUIRED(HttpStatus.BAD_REQUEST, "픽 여부는 필수입니다."),

	//REVIEW REPLY,
	REQUIRED_REVIEW_REPLY_CONTENT(HttpStatus.BAD_REQUEST, "댓글 내용은 필수 입력값입니다."),
	CONTENT_IS_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "댓글 내용은 1자 이상 500자 이하로 작성해주세요."),

	//REVIEW
	REVIEW_ID_REQUIRED(HttpStatus.BAD_REQUEST, "reviewId(식별자)는 필수입니다."),
	REVIEW_DISPLAY_STATUS_NOT_EMPTY(HttpStatus.BAD_REQUEST, "리뷰 공개/비공개상태는 필수입니다."),
	REVIEW_ID_MINIMUM(HttpStatus.BAD_REQUEST, "리뷰 식별자는 최소 1 이상이어야 합니다."),
	REVIEW_IMAGE_ORDER_REQUIRED(HttpStatus.BAD_REQUEST, "리뷰 이미지 Order 값은 필수입니다"),
	REVIEW_IMAGE_URL_REQUIRED(HttpStatus.BAD_REQUEST, "리뷰 이미지 URL은 필수입니다"),
	REVIEW_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "리뷰 내용은 필수입니다."),
	REVIEW_CONTENT_MAXIMUM(HttpStatus.BAD_REQUEST, "리뷰 내용의 최대 글자수를 초과했습니다."),
	PRICE_MINIMUM(HttpStatus.BAD_REQUEST, "가격은 0원 이상이어야 합니다."),
	PRICE_MAXIMUM(HttpStatus.BAD_REQUEST, "입력할 수 있는 가격의 범위가 아닙니다."),
	INVALID_ZIP_CODE_PATTERN(HttpStatus.BAD_REQUEST, "우편번호는 숫자 5자리 형식입니다."),

	// FOLLOW,
	IS_NO_USER_ID_TO_FOLLOW(HttpStatus.BAD_REQUEST, "팔로우 할 유저의 아이디가 없습니다."),
	SELECT_FOLLOWING_OR_UNFOLLOW(HttpStatus.BAD_REQUEST, "FOLLOWING, UNFOLLOW 중 하나를 선택해주세요."),

	// LIKE,
	LIKE_STATUS_IS_REQUIRED(HttpStatus.BAD_REQUEST, "status는 필수입니다.(LIKE,DISLIKE)"),

	//RATING
	RATING_REQUIRED(HttpStatus.BAD_REQUEST, "별점은 필수입니다."),

	//USER,
	REQUIRED_USER_ID(HttpStatus.BAD_REQUEST, "유저 아이디는 필수입니다."),
	NICKNAME_PATTERN_NOT_VALID(HttpStatus.BAD_REQUEST, "닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다."),
	EMAIL_PATTERN_NOT_VALID(HttpStatus.BAD_REQUEST, "올바른 이메일형식이 아닙니다."),
	SOCIAL_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "로그인 및 회원가입에 필요한 소셜타입이 없습니다."),
	AGE_MINIMUM(HttpStatus.BAD_REQUEST, "나이는 0 이상 이어야 합니다."),
	EMAIL_NOT_BLANK(HttpStatus.BAD_REQUEST, "이메일은 필수입니다."),

	//REPORT,
	REPORT_TARGET_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "신고 대상자 아이디는 필수입니다."),
	REPORT_TYPE_NOT_VALID(HttpStatus.BAD_REQUEST, "신고 타입이 적절하지 않습니다. ( SPAM , INAPPROPRIATE_CONTENT " +
		",FRAUD ,COPYRIGHT_INFRINGEMENT ,OTHER )"),
	REPORT_CONTENT_MAX_SIZE(HttpStatus.BAD_REQUEST, "신고 내용은 300자 이내로 작성해주세요."),

	//LOCATION INFO
	STREET_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "도로명 주소는 필수입니다"),

	//HELP
	HELP_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "문의글 제목은 필수입니다."),
	HELP_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "문의글 내용은 필수입니다."),
	REQUIRED_HELP_TYPE(HttpStatus.BAD_REQUEST, "문의 유형은 필수입니다.(WHISKEY, REVIEW, USER, ETC)");

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
