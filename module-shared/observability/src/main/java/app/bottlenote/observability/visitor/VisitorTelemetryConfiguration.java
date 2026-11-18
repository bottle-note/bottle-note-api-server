package app.bottlenote.observability.visitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "bottlenote.observability.visitor-telemetry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@EnableConfigurationProperties(VisitorTelemetryProperties.class)
public class VisitorTelemetryConfiguration {

  @Bean
  VisitorTelemetryPublisher visitorTelemetryPublisher(
      StringRedisTemplate redisTemplate, VisitorTelemetryProperties properties) {
    return new RedisVisitorTelemetryPublisher(
        redisTemplate, properties.getStreamKey(), properties.getMaxLength());
  }

  @Bean
  @ConditionalOnBean(VisitorTelemetryFilter.class)
  FilterRegistrationBean<VisitorTelemetryFilter> visitorTelemetryFilterRegistration(
      VisitorTelemetryFilter filter) {
    FilterRegistrationBean<VisitorTelemetryFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
