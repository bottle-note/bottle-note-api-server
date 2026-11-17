package app.bottlenote.support.business.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;

@Getter
public class BusinessSupportException extends AbstractCustomException {

  public BusinessSupportException(ExceptionCode exceptionCode) {
    super(exceptionCode);
  }
}
