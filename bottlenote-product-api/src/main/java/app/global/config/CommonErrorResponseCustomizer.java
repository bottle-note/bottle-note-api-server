package app.global.config;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.exception.custom.code.ExceptionCode;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.global.security.jwt.JwtExceptionType;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import app.global.security.SecurityPolicyConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * 어느 엔드포인트에서나 돌아올 수 있는 공통 오류 응답을 문서에 싣는다.
 *
 * <p>대상은 전역 예외 처리기가 도메인과 무관하게 만들어내는 응답뿐이다. 요청 값 검증 실패, 토큰 문제, 처리하지 못한 서버 오류가 여기에 해당한다. 도메인마다 다른 업무
 * 규칙 위반은 각 엔드포인트가 직접 문서화한다.
 *
 * <p>엔드포인트가 같은 상태 코드를 이미 설명해 두었다면 그 설명을 덮지 않는다.
 */
@Component
public class CommonErrorResponseCustomizer implements OperationCustomizer, OpenApiCustomizer {

  private static final String BAD_REQUEST = "400";
  private static final String UNAUTHORIZED = "401";
  private static final String FORBIDDEN = "403";
  private static final String SERVER_ERROR = "500";

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    ErrorResponseSchema.registerInto(openApi.getComponents());
  }

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      return operation;
    }

    describe(
        responses,
        BAD_REQUEST,
        ValidExceptionCode.TYPE_MISMATCH,
        "요청 값이 올바르지 않습니다. 필수값 누락, 타입 불일치, 본문 파싱 실패가 여기에 해당합니다.");
    if (requiresToken(handlerMethod)) {
      describe(responses, UNAUTHORIZED, JwtExceptionType.MALFORMED_TOKEN, "액세스 토큰이 없거나 유효하지 않습니다.");
      // 만료된 토큰만 유일하게 403이다. JwtExceptionType.EXPIRED_TOKEN 참고
      describe(responses, FORBIDDEN, JwtExceptionType.EXPIRED_TOKEN, "액세스 토큰이 만료되었습니다.");
    }
    describe(responses, SERVER_ERROR, ValidExceptionCode.UNKNOWN_ERROR, "서버가 처리하지 못한 오류입니다.");

    return operation;
  }

  private void describe(
      ApiResponses responses, String status, ExceptionCode sample, String description) {
    responses.computeIfAbsent(status, code -> ErrorResponseSchema.response(sample, description));
  }

  /** 토큰을 요구하는 엔드포인트만 401을 낼 수 있다. 판정 근거는 실제 인증과 같은 {@code @SecurityPolicy}다. */
  private boolean requiresToken(HandlerMethod handlerMethod) {
    return SecurityPolicyRouteCollector.resolveAuthType(
            handlerMethod, SecurityPolicyConfig.FALLBACK_AUTH_TYPE)
        != AuthType.PUBLIC;
  }
}
