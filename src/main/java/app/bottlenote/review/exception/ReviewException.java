package app.bottlenote.review.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;

@Getter
public class ReviewException extends AbstractCustomException {

	public ReviewException(ExceptionCode code) {
		super(code);
	}
}
