package app.bottlenote.shared.exception.custom;

import app.bottlenote.shared.exception.custom.code.CustomExceptionCode;
import lombok.Getter;

@Getter
public class CustomException extends AbstractCustomException {
	public CustomException(CustomExceptionCode exceptionCode) {
		super(exceptionCode);
	}
}