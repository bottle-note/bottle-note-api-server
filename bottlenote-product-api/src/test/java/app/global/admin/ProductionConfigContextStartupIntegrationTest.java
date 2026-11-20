package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import app.ProductApplication;
import app.bottlenote.accesscontrol.scheduled.IpBanJobConfiguration;
import app.bottlenote.curation.config.CurationSpecResourceSyncRunner;
import app.bottlenote.curation.domain.CurationSpecRepository;
import app.bottlenote.global.pagination.CursorSecretValidator;
import app.bottlenote.global.security.accesscontrol.AccessControlFilter;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties;
import app.global.admin.security.AdminAccessControlFilter;
import app.global.admin.security.AdminApiRequestMatcher;
import com.redis.testcontainers.RedisContainer;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.MultipartConfigElement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 운영 application.yml로 통합 컨텍스트를 띄운다.
 *
 * <p>테스트 공통 설정(application-test.yml)은 bean 덮어쓰기를 허용하고, 공통 컨테이너 설정(TestContainersConfig)도 운영 bean을
 * 같은 이름으로 대체한다. 그래서 운영에서만 드러나는 bean 이름 충돌을 잡으려면 이 테스트는 둘 다 쓰지 않고 운영 기본값(덮어쓰기 금지)으로 기동한다. 테스트
 * classpath의 빈 application.yml이 운영 파일을 가리므로 운영 파일을 추가 위치로 읽는다.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(
    classes = ProductApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.config.additional-location=file:src/main/resources/application.yml",
      "spring.main.allow-bean-definition-overriding=false",
      // 운영에서 환경 변수로 주입하는 값. 실제 값과 무관하게 형식만 맞춘 더미다
      "JWT_SECRET_KEY=c2VjdXJlU2VjcmV0S2V5MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6QWJDRGVGR2hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlrSg==",
      "KAKAO_APP_ID=1052783",
      "PROFANITY_FILTER_KEY=dummy-profanity-key",
      "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://127.0.0.1:1/v1/traces",
      "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://127.0.0.1:1/v1/metrics",
      "OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://127.0.0.1:1/v1/logs",
      // 기동만 확인한다. 클러스터 Quartz 작업이 테스트 중에 돌지 않게 스케줄러 시작만 끈다
      "spring.quartz.auto-startup=false"
    })
@DisplayName("[integration] 운영 설정 통합 컨텍스트 기동")
class ProductionConfigContextStartupIntegrationTest {

  @Container @ServiceConnection
  static MySQLContainer<?> mysql =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
          .withDatabaseName("bottlenote_production_config")
          .withUsername("root")
          .withPassword("root");

  @Container @ServiceConnection
  static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.0.12"));

  @Autowired private ApplicationContext context;
  @Autowired private Environment environment;
  @Autowired private FilterChainProxy filterChainProxy;
  @Autowired private CurationSpecRepository curationSpecRepository;

  @Test
  @DisplayName("운영 설정을 읽어 bean 덮어쓰기 없이 기동할 때 product와 admin 설정이 함께 적용된다")
  void 운영_설정으로_덮어쓰기_없이_기동한다() {
    assertThat(environment.getProperty("spring.main.allow-bean-definition-overriding"))
        .isEqualTo("false");
    assertThat(environment.getProperty("app.type")).isEqualTo("product");
    assertThat(environment.getProperty("springdoc.api-docs.path"))
        .isEqualTo("/api/v1/openapi.product.json");
    assertThat(environment.getProperty("app.cors.admin.allowed-origins[0]"))
        .isEqualTo("http://localhost:5173");

    // app.type=product 조건 bean이 그대로 등록된다
    assertThat(context.getBeansOfType(IpBanJobConfiguration.class)).hasSize(1);
    assertThat(context.getBeansOfType(CursorSecretValidator.class)).hasSize(1);
    // springdoc의 기본 문서는 OpenAPI bean 하나를 기준으로 만든다
    assertThat(context.getBeansOfType(OpenAPI.class)).hasSize(1);
  }

  @Test
  @DisplayName("보안 체인을 조회할 때 admin 체인이 먼저 평가되고 product 체인은 마지막 any-request 체인이다")
  void admin_체인이_product_체인보다_먼저_평가된다() {
    assertThat(context.getBeansOfType(SecurityFilterChain.class))
        .containsOnlyKeys("adminFilterChain", "filterChain");

    List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
    assertThat(chains).hasSize(2);
    assertThat(((DefaultSecurityFilterChain) chains.get(0)).getRequestMatcher())
        .isSameAs(AdminApiRequestMatcher.INSTANCE);
    assertThat(((DefaultSecurityFilterChain) chains.get(1)).getRequestMatcher())
        .isSameAs(AnyRequestMatcher.INSTANCE);
  }

  @Test
  @DisplayName(
      "보안 체인의 필터를 조회할 때 admin 체인은 기존 admin 앱 설정의 접근 제어를, product 체인은 product 설정의 접근 제어를 쓴다")
  void 체인마다_접근_제어_설정이_다르다() {
    AccessControlProperties admin =
        context.getBean("adminAccessControlProperties", AccessControlProperties.class);
    // admin 설정 bean은 autowire 후보가 아니므로 타입 조회는 product 설정 하나로 정해진다
    AccessControlProperties product = context.getBean(AccessControlProperties.class);

    assertThat(admin.getKeyNamespace()).isEqualTo("admin");
    assertThat(admin.getUnknownIpRateLimit().limit()).isEqualTo(20);
    assertThat(product.getKeyNamespace()).isEqualTo("product");
    assertThat(product.getUnknownIpRateLimit().limit()).isEqualTo(30);

    List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
    assertThat(chains.get(0).getFilters())
        .hasAtLeastOneElementOfType(AdminAccessControlFilter.class)
        .noneMatch(AccessControlFilter.class::isInstance);
    assertThat(chains.get(1).getFilters())
        .hasAtLeastOneElementOfType(AccessControlFilter.class)
        .noneMatch(AdminAccessControlFilter.class::isInstance);
  }

  @Test
  @DisplayName("JVM 전역 설정을 확인할 때 product의 SecurityContextHolder 전략이 admin 요청에도 적용된다")
  void SecurityContextHolder_전략은_JVM_전역이다() throws InterruptedException {
    // product SecurityConfig가 MODE_INHERITABLETHREADLOCAL로 바꾼다. admin 앱은 기본 전략이었으므로 차이를 테스트로 남긴다.
    Authentication authentication = new TestingAuthenticationToken("admin", "credentials");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
    AtomicReference<Authentication> seenByChild = new AtomicReference<>();
    try {
      Thread child =
          new Thread(() -> seenByChild.set(SecurityContextHolder.getContext().getAuthentication()));
      child.start();
      child.join();
    } finally {
      SecurityContextHolder.clearContext();
    }

    assertThat(seenByChild.get()).isSameAs(authentication);
  }

  @Test
  @DisplayName("multipart 설정을 확인할 때 admin 엑셀 업로드 한도와 즉시 파싱 기본 resolver를 쓴다")
  void multipart_설정은_admin_엑셀_한도를_쓴다() {
    MultipartConfigElement multipart = context.getBean(MultipartConfigElement.class);

    assertThat(multipart.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(5).toBytes());
    assertThat(multipart.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(6).toBytes());
    assertThat(context.getBean("multipartResolver"))
        .isExactlyInstanceOf(StandardServletMultipartResolver.class);
  }

  @Test
  @DisplayName("기동 작업을 확인할 때 큐레이션 스펙 리소스 동기화가 실행되어 스펙이 저장된다")
  void 큐레이션_스펙_동기화가_기동_시_실행된다() {
    assertThat(context.getBeansOfType(CurationSpecResourceSyncRunner.class)).hasSize(1);
    assertThat(curationSpecRepository.findByCode("RECOMMENDED_WHISKY")).isPresent();
  }
}
