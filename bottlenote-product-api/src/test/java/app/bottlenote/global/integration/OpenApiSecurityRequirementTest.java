package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import app.global.security.SecurityPolicyConfig;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** 문서에 표시된 인증 요구가 실제 보안 정책과 어긋나지 않는지 검사한다. */
@Tag("integration")
@DisplayName("[integration] OpenAPI 인증 요구 표시")
class OpenApiSecurityRequirementTest extends OpenApiSpecTestSupport {

  private static final String BEARER_AUTH = "bearerAuth";

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Test
  @DisplayName("문서의 토큰 요구는 실제 인증 정책과 일치한다")
  void 문서의_토큰_요구가_실제_정책과_일치한다() {
    var policies = authTypesByEndpoint();
    var operations = operationsOf(fetchSpec());

    // 경로 매칭이 실패하면 아래 검사가 조용히 비어버리므로 먼저 고정한다
    var unmatched =
        operations.stream()
            .map(SpecOperation::endpoint)
            .filter(endpoint -> !policies.containsKey(endpoint))
            .toList();
    assertThat(unmatched)
        .withFailMessage("실제 정책을 찾지 못한 엔드포인트가 있습니다:%n%s", joined(unmatched))
        .isEmpty();

    var violations =
        operations.stream()
            .filter(
                operation ->
                    requiresBearer(operation)
                        != (policies.get(operation.endpoint()) != AuthType.PUBLIC))
            .map(
                operation ->
                    "%s (정책 %s)"
                        .formatted(operation.endpoint(), policies.get(operation.endpoint())))
            .toList();

    assertThat(violations)
        .withFailMessage("문서의 토큰 요구가 실제 정책과 다릅니다:%n%s", joined(violations))
        .isEmpty();
  }

  @Test
  @DisplayName("토큰을 요구하는 엔드포인트가 문서에 실제로 존재한다")
  void 토큰을_요구하는_엔드포인트가_존재한다() {
    var requiring = operationsOf(fetchSpec()).stream().filter(this::requiresBearer).toList();

    assertThat(requiring)
        .withFailMessage("토큰 요구가 하나도 실리지 않았습니다. 보안 스키마가 연결되지 않은 상태입니다")
        .isNotEmpty();
  }

  @Test
  @DisplayName("토큰이 있으면 쓰고 없어도 동작하는 엔드포인트는 익명 호출도 허용으로 표시된다")
  void 선택적_인증은_익명_호출도_허용으로_표시된다() {
    var policies = authTypesByEndpoint();

    var violations =
        operationsOf(fetchSpec()).stream()
            .filter(operation -> policies.get(operation.endpoint()) == AuthType.OPTIONAL_AUTH)
            .filter(operation -> !allowsAnonymous(operation))
            .map(SpecOperation::endpoint)
            .toList();

    assertThat(violations)
        .withFailMessage("선택적 인증인데 익명 호출 허용이 빠졌습니다:%n%s", joined(violations))
        .isEmpty();
  }

  /** 실제 보안 정책이 판정한 엔드포인트별 인증 방식. */
  private Map<String, AuthType> authTypesByEndpoint() {
    Map<String, AuthType> policies = new HashMap<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (mapping, handlerMethod) -> {
              var patterns = mapping.getPathPatternsCondition();
              if (patterns == null) {
                return;
              }
              AuthType auth =
                  SecurityPolicyRouteCollector.resolveAuthType(
                      handlerMethod, SecurityPolicyConfig.FALLBACK_AUTH_TYPE);
              mapping
                  .getMethodsCondition()
                  .getMethods()
                  .forEach(
                      method ->
                          patterns
                              .getPatterns()
                              .forEach(
                                  pattern ->
                                      policies.put(
                                          "%s %s"
                                              .formatted(method.name(), pattern.getPatternString()),
                                          auth)));
            });
    return policies;
  }

  private boolean requiresBearer(SpecOperation operation) {
    for (JsonNode item : operation.security()) {
      if (item.has(BEARER_AUTH)) {
        return true;
      }
    }
    return false;
  }

  /** 토큰 없이 호출해도 된다는 표시(빈 요구사항)를 함께 갖는지. */
  private boolean allowsAnonymous(SpecOperation operation) {
    for (JsonNode item : operation.security()) {
      if (item.isEmpty()) {
        return true;
      }
    }
    return false;
  }
}
