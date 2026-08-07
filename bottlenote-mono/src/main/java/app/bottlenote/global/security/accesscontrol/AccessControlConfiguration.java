package app.bottlenote.global.security.accesscontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(AccessControlProperties.class)
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AccessControlConfiguration {

  @Bean
  @ConditionalOnMissingBean(StringRedisTemplate.class)
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlStore.class)
  public AccessControlStore accessControlStore(StringRedisTemplate stringRedisTemplate) {
    return new RedisAccessControlStore(stringRedisTemplate);
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlMetrics.class)
  public AccessControlMetrics accessControlMetrics(ObjectProvider<MeterRegistry> meterRegistry) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    return registry == null ? AccessControlMetrics.noop() : new AccessControlMetrics(registry);
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlService.class)
  public AccessControlService accessControlService(
      AccessControlStore accessControlStore,
      AccessControlProperties properties,
      AccessControlMetrics accessControlMetrics) {
    return new AccessControlService(accessControlStore, properties, accessControlMetrics);
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlFilter.class)
  public AccessControlFilter accessControlFilter(
      AccessControlService accessControlService, ObjectMapper objectMapper) {
    return new AccessControlFilter(accessControlService, objectMapper);
  }

  /** Security chain 전용 — 서블릿 컨테이너 자동 등록을 막는다 (VisitorTelemetry와 동일). */
  @Bean
  @ConditionalOnBean(AccessControlFilter.class)
  FilterRegistrationBean<AccessControlFilter> accessControlFilterRegistration(
      AccessControlFilter filter) {
    FilterRegistrationBean<AccessControlFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
