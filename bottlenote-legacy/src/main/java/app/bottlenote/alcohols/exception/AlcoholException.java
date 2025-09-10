package app.bottlenote.alcohols.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class AlcoholException extends AbstractCustomException {

  public AlcoholException(AlcoholExceptionCode code) {
    super(code);
  }
}
