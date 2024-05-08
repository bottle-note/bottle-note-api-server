package app.bottlenote.image.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class ImageException extends AbstractCustomException {

	public ImageException(ImageExceptionCode exceptionCode) {
		super(exceptionCode);
	}
}
