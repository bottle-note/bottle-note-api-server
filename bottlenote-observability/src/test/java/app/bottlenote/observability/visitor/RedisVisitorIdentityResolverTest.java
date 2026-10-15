package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@Tag("unit")
class RedisVisitorIdentityResolverTest {
  @Test
  @DisplayName("Redis가 실패할 때 요청마다 새 UUID로 계속 처리한다")
  void Redis_실패시_계속_처리한다() {
    var resolver =
        new RedisVisitorIdentityResolver(
            new StringRedisTemplate() {
              @Override
              public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
                throw new QueryTimeoutException("unavailable");
              }
            });
    String first = resolver.resolve("203.0.113.1", "Browser");
    assertThat(UUID.fromString(first).toString()).isEqualTo(first);
    assertThat(resolver.resolve("203.0.113.1", "Browser")).isNotEqualTo(first);
  }

  @Test
  @DisplayName("IP나 UA가 없을 때 Redis 없이 서로 다른 번호를 발급한다")
  void 식별_정보_누락은_공유하지_않는다() {
    var resolver =
        new RedisVisitorIdentityResolver(
            new StringRedisTemplate() {
              @Override
              public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
                throw new AssertionError("Redis must not be called");
              }
            });
    for (String missing : new String[] {null, "", " "}) {
      assertThat(resolver.resolve(missing, "Browser"))
          .isNotEqualTo(resolver.resolve(missing, "Browser"));
      assertThat(resolver.resolve("203.0.113.1", missing))
          .isNotEqualTo(resolver.resolve("203.0.113.1", missing));
    }
  }

  @Test
  @DisplayName("Redis가 잘못된 값을 반환할 때 새 UUID로 복구한다")
  void 잘못된_응답을_복구한다() {
    for (String value : new String[] {null, "invalid", "1-1-1-1-1"}) {
      var resolver =
          new RedisVisitorIdentityResolver(
              new StringRedisTemplate() {
                @Override
                public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
                  return script.getResultType().cast(value);
                }
              });
      String result = resolver.resolve("203.0.113.1", "Browser");
      assertThat(UUID.fromString(result).toString()).isEqualTo(result);
    }
  }

  @Test
  @DisplayName("공유 키를 만들 때 원문을 숨기고 IP 공백을 정리하며 전체 UA를 구분한다")
  void 공유_키는_해시로_구분한다() {
    String key = RedisVisitorIdentityResolver.key("203.0.113.1", "Browser/100");
    assertThat(key).matches("visitor-identity:bootstrap:v1:[a-f0-9]{64}");
    assertThat(key).isEqualTo(RedisVisitorIdentityResolver.key(" 203.0.113.1 ", "Browser/100"));
    assertThat(key).isNotEqualTo(RedisVisitorIdentityResolver.key("203.0.113.2", "Browser/100"));
    assertThat(key).isNotEqualTo(RedisVisitorIdentityResolver.key("203.0.113.1", "Browser/101"));
  }
}
