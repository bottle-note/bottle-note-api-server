package app.bottlenote.global.exception.custom.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ValidExceptionCode implements ExceptionCode {

	//COMMON
	//String.format("'%s' 필드는 '%s' 타입이 필요하지만, 잘못된 값 '%s'가 입력되었습니다.",String.format("'%s' 필드는 '%s' 타입이 필요하지만, 잘못된 값 '%s'가 입력되었습니다.",
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "잘못된 타입입니다."),

	//ALCOHOL
	ALCOHOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "알코올 식별자는 필수입니다."),
	ALCOHOL_ID_MINIMUM(HttpStatus.BAD_REQUEST, "알코올 식별자는 최소 1 이상 이어야 합니다."),

	//PICK
	IS_PICKED_REQUIRED(HttpStatus.BAD_REQUEST, "픽 여부는 필수입니다.");

	private final HttpStatus httpStatus;
	private String message;

	ValidExceptionCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

	public void updateMessage(String errorMessage) {
		this.message = errorMessage;
	}
}
