package app.bottlenote.user.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;


@Getter
public class UserException extends AbstractCustomException {
	public UserException(UserExceptionCode code) {
		super(code);
	}
}
