package app.bottlenote.history.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class UserHistoryException extends AbstractCustomException {

	public UserHistoryException(UserHistoryExceptionCode code) {
		super(code);
	}
}
