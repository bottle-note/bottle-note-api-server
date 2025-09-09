package app.bottlenote.review.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class ReviewException extends AbstractCustomException {

  public ReviewException(ReviewExceptionCode code) {
    super(code);
  }
}
