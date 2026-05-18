package app.bottlenote.curation.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class CurationException extends AbstractCustomException {

  public CurationException(CurationExceptionCode code) {
    super(code);
  }
}
