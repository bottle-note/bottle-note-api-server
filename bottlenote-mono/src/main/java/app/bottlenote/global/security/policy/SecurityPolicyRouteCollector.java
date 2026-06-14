package app.bottlenote.global.security.policy;

import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.policy.SecurityPolicyRoute.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

public final class SecurityPolicyRouteCollector {

  private SecurityPolicyRouteCollector() {}

  public static SecurityPolicyRegistry collect(
      RequestMappingHandlerMapping handlerMapping,
      AuthType fallback,
      List<SecurityPolicyRoute> extraRoutes) {
    return SecurityPolicyRegistry.of(
        collectRoutes(handlerMapping.getHandlerMethods(), fallback), extraRoutes);
  }

  public static SecurityPolicyRegistry collect(
      Map<RequestMappingInfo, HandlerMethod> handlerMethods, AuthType fallback) {
    return new SecurityPolicyRegistry(collectRoutes(handlerMethods, fallback));
  }

  private static List<SecurityPolicyRoute> collectRoutes(
      Map<RequestMappingInfo, HandlerMethod> handlerMethods, AuthType fallback) {
    List<SecurityPolicyRoute> routes = new ArrayList<>();
    handlerMethods.forEach(
        (mapping, handlerMethod) -> {
          SecurityPolicy policy = findPolicy(handlerMethod);
          AuthType auth = policy == null ? fallback : policy.auth();
          Source source = policy == null ? Source.FALLBACK : Source.ANNOTATION;
          Set<String> methods =
              mapping.getMethodsCondition().getMethods().stream()
                  .map(RequestMethod::name)
                  .collect(Collectors.toUnmodifiableSet());

          PathPatternsRequestCondition pathPatternsCondition = mapping.getPathPatternsCondition();
          if (pathPatternsCondition == null) {
            throw new IllegalStateException(
                "SecurityPolicy requires PathPattern based request mappings");
          }
          for (PathPattern pattern : pathPatternsCondition.getPatterns()) {
            routes.add(
                new SecurityPolicyRoute(
                    methods, pattern, auth, source, handlerMethod.getShortLogMessage()));
          }
        });
    return routes;
  }

  private static SecurityPolicy findPolicy(HandlerMethod handlerMethod) {
    SecurityPolicy methodPolicy =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SecurityPolicy.class);
    if (methodPolicy != null) {
      return methodPolicy;
    }
    return AnnotatedElementUtils.findMergedAnnotation(
        handlerMethod.getBeanType(), SecurityPolicy.class);
  }
}
