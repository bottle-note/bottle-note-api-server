package app.bottlenote.user.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;

public class FollowException extends AbstractCustomException {
  public FollowException(FollowExceptionCode code) {
    super(code);
  }
}
