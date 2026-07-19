package app.bottlenote.observability.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Tag("unit")
@DisplayName("[unit] VisitorTelemetry 구성")
class VisitorTelemetryConfigurationTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withBean(
              StringRedisTemplate.class,
              () -> new StringRedisTemplate(new LettuceConnectionFactory()))
          .withUserConfiguration(VisitorTelemetryConfiguration.class);

  @Test
  @DisplayName("기본 설정에서는 관련 빈을 생성하지 않는다")
  void 기본_설정에서는_비활성화한다() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(VisitorTelemetryFilter.class);
          assertThat(context).doesNotHaveBean(VisitorTelemetryPublisher.class);
        });
  }

  @Test
  @DisplayName("활성화하면 Publisher를 생성하고 Product 필터의 Servlet 자동 등록을 비활성화한다")
  void 활성화하면_Security_chain_연결용_필터를_생성한다() {
    contextRunner
        .withPropertyValues("bottlenote.observability.visitor-telemetry.enabled=true")
        .withBean(
            VisitorTelemetryFilter.class,
            () ->
                new VisitorTelemetryFilter(
                    telemetry -> {}, () -> null, request -> null))
        .run(
            context -> {
              assertThat(context).hasSingleBean(VisitorTelemetryFilter.class);
              assertThat(context).hasSingleBean(VisitorTelemetryPublisher.class);
              assertThat(context.getBean(VisitorTelemetryProperties.class))
                  .satisfies(
                      properties -> {
                        assertThat(properties.isEnabled()).isTrue();
                        assertThat(properties.getStreamKey()).isEqualTo("visitor-telemetry");
                        assertThat(properties.getMaxLength()).isEqualTo(100_000L);
                      });
              FilterRegistrationBean<?> registration =
                  context.getBean(
                      "visitorTelemetryFilterRegistration", FilterRegistrationBean.class);
              assertThat(registration.isEnabled()).isFalse();
            });
  }
}
