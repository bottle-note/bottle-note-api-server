package app.bottlenote.support.help.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;

@Getter
public class HelpException extends AbstractCustomException {

  public HelpException(ExceptionCode exceptionCode) {
    super(exceptionCode);
  }
}
