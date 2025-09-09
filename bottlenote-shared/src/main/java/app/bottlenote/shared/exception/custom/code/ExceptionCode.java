package app.bottlenote.shared.exception.custom.code;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
	String getMessage();

	HttpStatus getHttpStatus();
}