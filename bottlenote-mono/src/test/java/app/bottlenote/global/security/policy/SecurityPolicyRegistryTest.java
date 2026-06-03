package app.bottlenote.global.security.policy;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC;
import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.annotation.SecurityPolicy;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo.BuilderConfiguration;
import org.springframework.web.util.pattern.PathPatternParser;

@Tag("unit")
@DisplayName("[unit] SecurityPolicy handler mapping 수집")
class SecurityPolicyRegistryTest {

  @Test
  @DisplayName("메서드 어노테이션의 인증 정책을 수집한다")
  void collect_whenMethodAnnotated_resolvesMethodPolicy() throws Exception {
    SecurityPolicyRegistry registry =
        collect(mapping("GET", "/public"), handler(PolicyController.class, "publicEndpoint"));

    assertThat(registry.resolve("GET", "/public")).isEqualTo(PUBLIC);
  }

  @Test
  @DisplayName("클래스 어노테이션을 기본값으로 쓰고 메서드 어노테이션이 우선한다")
  void collect_whenClassAnnotated_methodPolicyOverridesClassPolicy() throws Exception {
    SecurityPolicyRegistry registry =
        collect(
            mapping("GET", "/optional"),
            handler(OptionalController.class, "optionalEndpoint"),
            mapping("POST", "/optional/command"),
            handler(OptionalController.class, "requiredEndpoint"));

    assertThat(registry.resolve("GET", "/optional")).isEqualTo(OPTIONAL_AUTH);
    assertThat(registry.resolve("POST", "/optional/command")).isEqualTo(REQUIRED_AUTH);
  }

  @Test
  @DisplayName("어노테이션이 없는 handler는 required-auth로 fallback 수집한다")
  void collect_whenAnnotationMissing_fallsBackToRequiredAuth() throws Exception {
    SecurityPolicyRegistry registry =
        collect(
            mapping("POST", "/fallback-required"),
            handler(PolicyController.class, "missingPolicyEndpoint"));

    assertThat(registry.resolve("POST", "/fallback-required")).isEqualTo(REQUIRED_AUTH);
  }

  @Test
  @DisplayName("PathPattern 기준으로 matrix variable 경로도 동일한 정책에 매칭한다")
  void resolve_whenPathContainsMatrixVariable_matchesSameControllerPattern() throws Exception {
    SecurityPolicyRegistry registry =
        collect(
            mapping("PUT", "/api/v1/likes"),
            handler(PolicyController.class, "missingPolicyEndpoint"));

    assertThat(registry.resolve("PUT", "/api/v1/likes;v=1")).isEqualTo(REQUIRED_AUTH);
  }

  private static SecurityPolicyRegistry collect(Object... mappingAndHandlers) {
    Map<RequestMappingInfo, HandlerMethod> mappings = new LinkedHashMap<>();
    for (int i = 0; i < mappingAndHandlers.length; i += 2) {
      mappings.put(
          (RequestMappingInfo) mappingAndHandlers[i], (HandlerMethod) mappingAndHandlers[i + 1]);
    }
    return SecurityPolicyRouteCollector.collect(mappings, SecurityPolicy.AuthType.REQUIRED_AUTH);
  }

  private static RequestMappingInfo mapping(String method, String path) {
    BuilderConfiguration options = new BuilderConfiguration();
    options.setPatternParser(PathPatternParser.defaultInstance);
    return RequestMappingInfo.paths(path)
        .methods(org.springframework.web.bind.annotation.RequestMethod.valueOf(method))
        .options(options)
        .build();
  }

  private static HandlerMethod handler(Class<?> controllerType, String methodName)
      throws Exception {
    Method method = controllerType.getDeclaredMethod(methodName);
    return new HandlerMethod(controllerType.getDeclaredConstructor().newInstance(), method);
  }

  private static class PolicyController {

    @SecurityPolicy(auth = PUBLIC)
    @GetMapping("/public")
    void publicEndpoint() {}

    @PostMapping("/fallback-required")
    void missingPolicyEndpoint() {}
  }

  @SecurityPolicy(auth = OPTIONAL_AUTH)
  @RequestMapping("/optional")
  private static class OptionalController {

    @GetMapping
    void optionalEndpoint() {}

    @SecurityPolicy(auth = REQUIRED_AUTH)
    @PostMapping("/command")
    void requiredEndpoint() {}
  }
}
