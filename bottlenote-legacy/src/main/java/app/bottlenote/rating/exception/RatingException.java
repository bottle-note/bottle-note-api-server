package app.bottlenote.rating.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class RatingException extends AbstractCustomException {
  public RatingException(RatingExceptionCode code) {
    super(code);
  }
}
