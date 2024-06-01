package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class AlcoholException extends AbstractCustomException {

	public AlcoholException(AlcoholExceptionCode code) {
		super(code);
	}
}
