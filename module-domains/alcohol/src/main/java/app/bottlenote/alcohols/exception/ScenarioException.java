package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.exception.custom.code.ExceptionCode;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

public class ScenarioException extends AbstractCustomException {
  public ScenarioException(ScenarioExceptionCode code) {
    super(code);
  }

  public ScenarioException(ScenarioExceptionCode code, String detail) {
    super(new DetailCode(code, code.getMessage() + " " + detail));
  }

  private record DetailCode(ScenarioExceptionCode code, String message) implements ExceptionCode {
    @JsonValue
    public String codeName() {
      return code.name();
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
      return code.getHttpStatus();
    }
  }

  public static ScenarioException invalid(String detail) {
    return new ScenarioException(ScenarioExceptionCode.SCENARIO_INVALID, detail);
  }
}
