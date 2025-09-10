package app.bottlenote.shared.exception.custom.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomExceptionCode implements ExceptionCode {
	INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다."),
	NULL_IS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "NULL은 허용되지 않습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
	NOT_VALID(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}