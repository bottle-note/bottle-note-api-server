package app.external.systemone.http;

import app.external.systemone.dto.response.SystemOneFailureType;
import lombok.Getter;

/** 공급자 계약 위반. 클라이언트 경계 밖으로 나가지 않고 Failure로 변환된다. */
@Getter
public class SystemOneContractException extends RuntimeException {
  private final SystemOneFailureType type;

  public SystemOneContractException(SystemOneFailureType type, String message) {
    this(type, message, null);
  }

  public SystemOneContractException(SystemOneFailureType type, String message, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  public static SystemOneContractException invalidResponse(String message) {
    return new SystemOneContractException(SystemOneFailureType.INVALID_RESPONSE, message);
  }
}
