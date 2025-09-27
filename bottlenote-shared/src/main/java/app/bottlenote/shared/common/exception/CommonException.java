package app.bottlenote.shared.common.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class CommonException extends AbstractCustomException {

  public CommonException(CommonExceptionCode code) {
    super(code);
  }
}
