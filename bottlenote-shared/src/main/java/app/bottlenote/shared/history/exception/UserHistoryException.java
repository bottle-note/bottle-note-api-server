package app.bottlenote.shared.history.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class UserHistoryException extends AbstractCustomException {

  public UserHistoryException(UserHistoryExceptionCode code) {
    super(code);
  }
}
