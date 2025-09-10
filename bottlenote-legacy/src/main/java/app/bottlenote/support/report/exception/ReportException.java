package app.bottlenote.support.report.exception;

import app.bottlenote.shared.exception.custom.AbstractCustomException;
import lombok.Getter;

@Getter
public class ReportException extends AbstractCustomException {
  public ReportException(ReportExceptionCode code) {
    super(code);
  }
}
