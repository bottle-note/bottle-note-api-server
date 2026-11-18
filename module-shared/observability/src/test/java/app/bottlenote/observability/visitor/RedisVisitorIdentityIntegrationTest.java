package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@Testcontainers
class RedisVisitorIdentityIntegrationTest {
  @Container
  static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.0.12"));

  private final List<LettuceConnectionFactory> factories = new ArrayList<>();
  private StringRedisTemplate first;
  private StringRedisTemplate second;

  @BeforeEach
  void setUp() {
    first = template();
    second = template();
  }

  @AfterEach
  void tearDown() {
    factories.forEach(LettuceConnectionFactory::destroy);
  }

  @Test
  @DisplayName("서로 다른 서버의 첫 32개 동시 요청에 같은 쿠키와 방문자 해시를 준다")
  void 서버간_동시_요청은_하나의_번호를_받는다() throws Exception {
    String ua = "Browser/" + UUID.randomUUID();
    var events = new ConcurrentLinkedQueue<VisitorTelemetry>();
    var filters =
        List.of(
            new VisitorTelemetryFilter(
                events::add,
                () -> null,
                req -> "203.0.113.1",
                new RedisVisitorIdentityResolver(first)),
            new VisitorTelemetryFilter(
                events::add,
                () -> null,
                req -> "203.0.113.1",
                new RedisVisitorIdentityResolver(second)));
    CountDownLatch ready = new CountDownLatch(32);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(32)) {
      List<Future<String>> responses = new ArrayList<>();
      for (int i = 0; i < 32; i++) {
        var filter = filters.get(i % 2);
        responses.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                  var request = new MockHttpServletRequest("GET", "/api/v1/alcohols/5004");
                  request.addHeader(HttpHeaders.USER_AGENT, ua);
                  var response = new MockHttpServletResponse();
                  filter.doFilter(request, response, (req, res) -> {});
                  return response.getHeader(HttpHeaders.SET_COOKIE);
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<String> cookies = new ArrayList<>();
      for (var result : responses) cookies.add(result.get(10, TimeUnit.SECONDS));
      assertThat(cookies).doesNotContainNull().containsOnly(cookies.getFirst());
    }
    assertThat(events).hasSize(32);
    assertThat(events.stream().map(VisitorTelemetry::visitorId).distinct().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("공유 번호를 재사용할 때 만료를 연장하지 않고 3초가 지나면 새 번호를 준다")
  void 고정_만료_후_새_번호를_발급한다() {
    String ua = "Browser/" + UUID.randomUUID();
    String key = RedisVisitorIdentityResolver.key("203.0.113.1", ua);
    var resolver = new RedisVisitorIdentityResolver(first);
    String original = resolver.resolve("203.0.113.1", ua);
    assertThat(first.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, 3000L);
    await()
        .atMost(Duration.ofSeconds(3))
        .until(() -> first.getExpire(key, TimeUnit.MILLISECONDS) < 2000);
    long before = first.getExpire(key, TimeUnit.MILLISECONDS);
    assertThat(new RedisVisitorIdentityResolver(second).resolve("203.0.113.1", ua))
        .isEqualTo(original);
    assertThat(first.getExpire(key, TimeUnit.MILLISECONDS)).isLessThanOrEqualTo(before);
    await().atMost(Duration.ofSeconds(4)).until(() -> !Boolean.TRUE.equals(first.hasKey(key)));
    assertThat(resolver.resolve("203.0.113.1", ua)).isNotEqualTo(original);
  }

  @Test
  @DisplayName("IP 또는 전체 UA가 다르면 별도 번호를 발급한다")
  void 다른_환경은_구분한다() {
    var resolver = new RedisVisitorIdentityResolver(first);
    String ua = "Browser/" + UUID.randomUUID();
    String id = resolver.resolve("203.0.113.1", ua);
    assertThat(resolver.resolve("203.0.113.2", ua)).isNotEqualTo(id);
    assertThat(resolver.resolve("203.0.113.1", ua + "2")).isNotEqualTo(id);
    assertThat(resolver.resolve(" 203.0.113.1 ", ua)).isEqualTo(id);
  }

  private StringRedisTemplate template() {
    var factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getFirstMappedPort());
    factory.afterPropertiesSet();
    factory.start();
    factories.add(factory);
    return new StringRedisTemplate(factory);
  }
}
