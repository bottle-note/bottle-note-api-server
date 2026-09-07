package app.bottlenote.global.timeseries;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class TimeSeriesException extends AbstractCustomException {

  public TimeSeriesException(TimeSeriesExceptionCode code) {
    super(code);
  }
}
