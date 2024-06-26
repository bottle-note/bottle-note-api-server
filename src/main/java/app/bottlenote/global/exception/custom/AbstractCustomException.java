package app.bottlenote.global.exception.custom;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;



@Getter
public abstract class AbstractCustomException extends RuntimeException {
	private final ExceptionCode exceptionCode;

	protected AbstractCustomException(ExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}

	@Override
	public String getMessage() {
		return exceptionCode.getMessage();
	}
}
