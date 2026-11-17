package app.bottlenote.global.pagination;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class PaginationException extends AbstractCustomException {

  public PaginationException(PaginationExceptionCode code) {
    super(code);
  }
}
