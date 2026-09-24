package app.external.systemone.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemOneFailureType {
  NOT_CONFIGURED("API Key 등 공급자 설정이 없음", false),
  INVALID_REQUEST("공급자가 요청을 거부함 (400, 422)", false),
  UNAUTHORIZED("인증 실패 (401, 403)", false),
  RATE_LIMITED("요청 한도 초과 (429)", true),
  PROVIDER_UNAVAILABLE("공급자 서버 오류 또는 과부하 (5xx, 529)", true),
  TIMEOUT("연결 또는 응답 시간 초과", true),
  TRANSPORT_ERROR("연결 실패 등 전송 오류", true),
  INVALID_RESPONSE("응답을 해석할 수 없거나 계약과 다름", false),
  UNSUPPORTED_VERSION("지원하지 않는 모델 버전", false);

  private final String description;
  private final boolean retryable;
}
