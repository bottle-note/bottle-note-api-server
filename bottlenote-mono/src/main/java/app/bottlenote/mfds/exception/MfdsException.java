package app.bottlenote.mfds.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class MfdsException extends AbstractCustomException {

  public MfdsException(MfdsExceptionCode code) {
    super(code);
  }
}
