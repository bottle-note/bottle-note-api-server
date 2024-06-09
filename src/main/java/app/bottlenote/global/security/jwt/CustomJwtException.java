package app.bottlenote.global.security.jwt;

import app.bottlenote.global.exception.custom.AbstractCustomException;

public class CustomJwtException extends AbstractCustomException {

	protected CustomJwtException(CustomJwtExceptionCode exceptionCode) {
		super(exceptionCode);
	}
}
