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

	//ALCOHOL
	ALCOHOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "알코올 식별자는 필수입니다."),
	ALCOHOL_ID_MINIMUM(HttpStatus.BAD_REQUEST, "알코올 식별자는 최소 1 이상 이어야 합니다."),

	//PICK
	IS_PICKED_REQUIRED(HttpStatus.BAD_REQUEST, "픽 여부는 필수입니다.");

	private final HttpStatus httpStatus;
	private String message;

	public ValidExceptionCode message(String errorMessage) {
		this.message = errorMessage;
		return this;
	}
}
