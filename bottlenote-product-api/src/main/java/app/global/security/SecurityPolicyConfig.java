package app.global.security;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;

import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.global.security.policy.SecurityPolicyRoute;
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class SecurityPolicyConfig {

  @Bean
  SecurityPolicyRegistry securityPolicyRegistry(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
    return SecurityPolicyRouteCollector.collect(
        handlerMapping,
        REQUIRED_AUTH,
        List.of(
            SecurityPolicyRoute.explicit(null, "/actuator/**", PUBLIC),
            SecurityPolicyRoute.explicit(null, "/error", PUBLIC),
            // API 스펙은 클라이언트 개발자가 인증 없이 받아갈 수 있어야 한다
            SecurityPolicyRoute.explicit(null, "/openapi.product.json", PUBLIC)));
  }
}
