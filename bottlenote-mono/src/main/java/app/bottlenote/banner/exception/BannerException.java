package app.bottlenote.banner.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class BannerException extends AbstractCustomException {

  public BannerException(BannerExceptionCode code) {
    super(code);
  }
}
