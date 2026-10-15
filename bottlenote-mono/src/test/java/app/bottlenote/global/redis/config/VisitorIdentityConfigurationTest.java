package app.bottlenote.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.observability.visitor.VisitorIdentityResolver;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
class VisitorIdentityConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(
              "redisConnectionFactory",
              LettuceConnectionFactory.class,
              LettuceConnectionFactory::new)
          .withUserConfiguration(VisitorIdentityConfiguration.class);

  @Test
  @DisplayName("수집이 비활성화된 경우 전용 발급기를 생성하지 않는다")
  void 비활성화시_발급기를_만들지_않는다() {
    runner.run(context -> assertThat(context).doesNotHaveBean(VisitorIdentityResolver.class));
  }

  @Test
  @DisplayName("활성화하면 공용 연결을 유지하며 200ms 전용 연결을 생성하고 종료한다")
  void 전용_연결을_소유하고_종료한다() {
    AtomicReference<LettuceConnectionFactory> owned = new AtomicReference<>();
    runner
        .withPropertyValues("bottlenote.observability.visitor-telemetry.enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(VisitorIdentityResolver.class);
              assertThat(context).hasSingleBean(RedisConnectionFactory.class);
              var configuration = context.getBean(VisitorIdentityConfiguration.class);
              var factory =
                  (LettuceConnectionFactory)
                      ReflectionTestUtils.getField(configuration, "ownedFactory");
              owned.set(factory);
              assertThat(factory).isNotSameAs(context.getBean(RedisConnectionFactory.class));
              assertThat(factory.getClientConfiguration().getCommandTimeout())
                  .isEqualTo(Duration.ofMillis(200));
              assertThat(factory.isRunning()).isTrue();
            });
    assertThat(owned.get().isRunning()).isFalse();
  }
}
