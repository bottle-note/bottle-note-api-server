package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import app.bottlenote.global.security.accesscontrol.AccessControlFilter;
import app.bottlenote.global.security.accesscontrol.AccessControlMetrics;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.PathRateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties.RateLimitRule;
import app.bottlenote.global.security.accesscontrol.AccessControlService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import app.bottlenote.global.security.accesscontrol.BanSnapshotHolder;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import app.global.admin.security.AdminAccessControlConfig;
import app.global.admin.security.AdminAccessControlFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * admin 체인의 IP ban·rate limit을 운영 application.yml 값으로 검증한다.
 *
 * <p>기존 admin 앱은 context-path(/admin/api)를 뺀 경로로 규칙을 골랐다. 통합 앱은 mount를 MVC와 같은 세그먼트 해석으로 빼서, 인코딩을
 * 바꾼 경로도 정상 경로와 같은 한도, 같은 버킷, 같은 관리 평면 예외를 받아야 한다.
 */
@Tag("unit")
@DisplayName("[unit] admin 체인 접근 제어")
class AdminAccessControlFilterTest {

  private static final String CLIENT_IP = "203.0.113.10";
  private static final String UNKNOWN_IP = "unknown";

  private RecordingAccessControlStore store;
  private AdminAccessControlFilter filter;

  @BeforeEach
  void setUp() throws IOException {
    AccessControlProperties properties = bindProduction(AdminAccessControlConfig.PROPERTIES_PREFIX);
    store = new RecordingAccessControlStore();
    AccessControlService service =
        new AccessControlService(
            store,
            properties,
            AccessControlMetrics.noop(),
            new BanSnapshotHolder(properties.getSnapshot().getMaxEntries()),
            Clock.systemUTC());
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    filter = new AdminAccessControlFilter(new AccessControlFilter(service, objectMapper));
  }

  @Test
  @DisplayName("운영 설정을 읽을 때 기존 admin 앱의 네임스페이스, 한도, 경로 규칙, 관리 평면 예외와 같다")
  void admin_설정은_기존_admin_앱_값이다() throws IOException {
    AccessControlProperties admin = bindProduction(AdminAccessControlConfig.PROPERTIES_PREFIX);
    AccessControlProperties product = bindProduction("bottlenote.access-control");

    assertThat(admin.getKeyNamespace()).isEqualTo("admin").isNotEqualTo(product.getKeyNamespace());
    assertThat(admin.isFailOpen()).isTrue();
    assertThat(admin.getDefaultRateLimit()).isEqualTo(new RateLimitRule(300, 60));
    assertThat(admin.getUnknownIpRateLimit()).isEqualTo(new RateLimitRule(20, 60));
    assertThat(admin.getPathRules())
        .extracting(
            PathRateLimitRule::getPathPrefix,
            PathRateLimitRule::getMethods,
            PathRateLimitRule::getLimit,
            PathRateLimitRule::getWindowSeconds)
        .containsExactly(tuple("/v1/auth", List.of(), 30, 60));
    assertThat(admin.getManagementPathPrefixes()).containsExactly("/v1/access-control");
    assertThat(admin.getExcludedPathPrefixes())
        .containsExactly("/actuator", "/v1/openapi.admin.json");
    // product 설정에는 admin 경로 규칙이 섞이지 않는다
    assertThat(product.getPathRules())
        .extracting(PathRateLimitRule::getPathPrefix)
        .noneMatch(prefix -> prefix.startsWith("/admin"));
    assertThat(product.getManagementPathPrefixes()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/api/v1/auth/login",
        "/admin/%61pi/v1/auth/login",
        "/%61dmin/api/v1/auth/login",
        "/admin/api/%761/auth/login",
        "/admin/api/v1/%61uth/login",
        "/admin/api/v1/auth/%6Cogin"
      })
  @DisplayName("인코딩을 바꾼 로그인 경로를 호출할 때 정상 경로와 같은 30회 한도와 버킷을 쓴다")
  void 인코딩_변형_로그인_경로는_같은_한도와_버킷을_쓴다(String path) throws Exception {
    Result result = call("POST", path, CLIENT_IP);

    assertThat(result.passed()).isTrue();
    assertThat(result.limit()).isEqualTo("30");
    assertThat(store.lastKey()).isEqualTo("admin:" + CLIENT_IP + "|/v1/auth");
  }

  @Test
  @DisplayName("정상 경로와 인코딩 변형을 섞어 로그인할 때 한 버킷에서 세어 31번째에 429를 준다")
  void 로그인_한도는_인코딩_변형과_합쳐_30회다() throws Exception {
    for (int i = 0; i < 30; i++) {
      String path = i % 2 == 0 ? "/admin/api/v1/auth/login" : "/admin/%61pi/v1/auth/login";
      Result result = call("POST", path, CLIENT_IP);
      assertThat(result.passed()).as("%d번째 요청", i + 1).isTrue();
      assertThat(result.remaining()).isEqualTo(String.valueOf(29 - i));
    }

    Result limited = call("POST", "/admin/%61pi/v1/auth/login", CLIENT_IP);

    assertThat(limited.passed()).isFalse();
    assertThat(limited.status()).isEqualTo(429);
    assertThat(limited.limit()).isEqualTo("30");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/admin/api/v1/users", "/admin/%61pi/v1/users", "/admin/api/v1/%75sers"})
  @DisplayName("일반 admin API를 호출할 때 인코딩과 관계없이 admin 기본 한도 300과 admin 버킷을 쓴다")
  void 일반_admin_API는_admin_기본_한도를_쓴다(String path) throws Exception {
    Result result = call("GET", path, CLIENT_IP);

    assertThat(result.passed()).isTrue();
    assertThat(result.limit()).isEqualTo("300");
    assertThat(store.lastKey()).isEqualTo("admin:" + CLIENT_IP + "|default");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/api/v1/access-control/ip-bans",
        "/admin/%61pi/v1/access-control/ip-bans",
        "/admin/api/v1/%61ccess-control/ip-bans"
      })
  @DisplayName("차단된 IP가 관리 평면을 호출할 때 인코딩과 관계없이 ban과 rate limit을 모두 건너뛴다")
  void 관리_평면은_인코딩_변형도_ban과_한도를_건너뛴다(String path) throws Exception {
    store.ban(CLIENT_IP, Duration.ofMinutes(10), "self-lockout");

    Result management = call("GET", path, CLIENT_IP);
    Result other = call("GET", "/admin/%61pi/v1/users", CLIENT_IP);

    assertThat(management.passed()).isTrue();
    assertThat(management.limit()).isNull();
    assertThat(other.passed()).isFalse();
    assertThat(other.status()).isEqualTo(403);
    assertThat(store.consumedKeys()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/admin/api/v1/users", "/admin/%61pi/v1/auth/login"})
  @DisplayName("IP를 해석하지 못한 admin 요청을 받을 때 admin unknown-IP 한도 20과 product와 다른 버킷을 쓴다")
  void IP를_해석하지_못하면_admin_unknown_한도를_쓴다(String path) throws Exception {
    for (int i = 0; i < 20; i++) {
      Result result = call("POST", path, UNKNOWN_IP);
      assertThat(result.passed()).isTrue();
      assertThat(result.limit()).isEqualTo("20");
    }

    Result limited = call("POST", path, UNKNOWN_IP);

    assertThat(limited.status()).isEqualTo(429);
    assertThat(store.consumedKeys()).containsOnly("admin:_unknown|unknown");
  }

  @Test
  @DisplayName("admin 문서 경로를 호출할 때 기존 admin 앱처럼 rate limit에서 제외한다")
  void admin_문서는_rate_limit에서_제외한다() throws Exception {
    Result result = call("GET", "/admin/api/v1/openapi.admin.json", CLIENT_IP);

    assertThat(result.passed()).isTrue();
    assertThat(result.limit()).isNull();
    assertThat(store.consumedKeys()).isEmpty();
  }

  @Test
  @DisplayName("허용된 요청을 다음 필터로 넘길 때 경로를 바꾸지 않은 원래 요청을 넘긴다")
  void 다음_필터에는_원래_요청을_넘긴다() throws Exception {
    MockHttpServletRequest request = request("GET", "/admin/%61pi/v1/users", CLIENT_IP);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(request.getRequestURI()).isEqualTo("/admin/%61pi/v1/users");
  }

  private Result call(String method, String path, String ip) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request(method, path, ip), response, chain);
    return new Result(
        chain.getRequest() != null,
        response.getStatus(),
        response.getHeader("X-RateLimit-Limit"),
        response.getHeader("X-RateLimit-Remaining"));
  }

  private static MockHttpServletRequest request(String method, String path, String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(ip);
    return request;
  }

  // 테스트 classpath의 application.yml은 운영 파일을 가리므로 운영 파일을 직접 읽는다. 첫 문서가 프로파일 조건 없는 기본 설정이다.
  private static AccessControlProperties bindProduction(String prefix) throws IOException {
    List<PropertySource<?>> documents =
        new YamlPropertySourceLoader()
            .load("application", new FileSystemResource("src/main/resources/application.yml"));
    return new Binder(ConfigurationPropertySources.from(documents.get(0)))
        .bind(prefix, AccessControlProperties.class)
        .get();
  }

  private record Result(boolean passed, int status, String limit, String remaining) {}

  // rate-limit 버킷 키를 기록한다. 같은 버킷인지 키로 확인하기 위해서다.
  private static final class RecordingAccessControlStore extends InMemoryAccessControlStore {

    private final List<String> keys = new ArrayList<>();

    @Override
    public AccessControlStore.ConsumeResult tryConsume(String key, int limit, Duration window) {
      keys.add(key);
      return super.tryConsume(key, limit, window);
    }

    private String lastKey() {
      return keys.isEmpty() ? null : keys.get(keys.size() - 1);
    }

    private List<String> consumedKeys() {
      return keys;
    }
  }
}
