package app.bottlenote.global.exception.custom;

import app.bottlenote.global.exception.custom.code.CustomExceptionCode;
import lombok.Getter;


@Getter
public class CustomException extends AbstractCustomException {
	public CustomException(CustomExceptionCode exceptionCode) {
		super(exceptionCode);
	}
}
