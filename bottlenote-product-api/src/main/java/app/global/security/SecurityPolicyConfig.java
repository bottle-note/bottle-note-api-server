package app.global.security;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.global.security.policy.SecurityPolicyRoute;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class SecurityPolicyConfig {

  /** {@code @SecurityPolicy}를 선언하지 않은 엔드포인트에 적용되는 인증 방식. */
  public static final AuthType FALLBACK_AUTH_TYPE = REQUIRED_AUTH;

  @Bean
  SecurityPolicyRegistry securityPolicyRegistry(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      @Value("${springdoc.api-docs.path}") String specPath) {
    return SecurityPolicyRouteCollector.collect(
        handlerMapping,
        FALLBACK_AUTH_TYPE,
        List.of(
            publicRoute("/actuator/**"),
            publicRoute("/error"),
            // API 스펙은 클라이언트 개발자가 인증 없이 받아갈 수 있어야 한다
            publicRoute(specPath)));
  }

  /** HTTP 메서드를 구분하지 않고 전체 공개하는 경로. */
  private static SecurityPolicyRoute publicRoute(String pattern) {
    return SecurityPolicyRoute.explicit(null, pattern, PUBLIC);
  }
}
