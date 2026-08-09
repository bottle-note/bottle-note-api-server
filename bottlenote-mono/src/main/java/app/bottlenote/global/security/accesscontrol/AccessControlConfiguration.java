package app.bottlenote.global.security.accesscontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(AccessControlProperties.class)
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AccessControlConfiguration {

  /** 공용 StringRedisTemplate. access-control 전용 template/factory는 Bean으로 등록하지 않는다. */
  @Bean
  @ConditionalOnMissingBean(StringRedisTemplate.class)
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }

  /**
   * 전용 Lettuce factory(200ms timeout)와 StringRedisTemplate을 비등록으로 구성해 store가 소유한다. Spring {@link
   * RedisConnectionFactory} autowire 후보를 늘리지 않는다.
   */
  @Bean(destroyMethod = "destroy")
  @ConditionalOnMissingBean(AccessControlStore.class)
  public AccessControlStore accessControlStore(
      RedisConnectionFactory redisConnectionFactory, AccessControlProperties properties) {
    LettuceConnectionFactory dedicated =
        createDedicatedConnectionFactory(redisConnectionFactory, properties);
    // 비등록 인스턴스 — Spring lifecycle 대신 수동 초기화/종료
    dedicated.afterPropertiesSet();
    dedicated.start();
    StringRedisTemplate template = new StringRedisTemplate(dedicated);
    template.afterPropertiesSet();
    return new RedisAccessControlStore(template, dedicated);
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlMetrics.class)
  public AccessControlMetrics accessControlMetrics(ObjectProvider<MeterRegistry> meterRegistry) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    return registry == null ? AccessControlMetrics.noop() : new AccessControlMetrics(registry);
  }

  @Bean
  @ConditionalOnMissingBean(BanSnapshotHolder.class)
  public BanSnapshotHolder banSnapshotHolder(AccessControlProperties properties) {
    return new BanSnapshotHolder(properties.getSnapshot().getMaxEntries());
  }

  @Bean
  @ConditionalOnMissingBean(BanSnapshotRefresher.class)
  public BanSnapshotRefresher banSnapshotRefresher(
      AccessControlStore accessControlStore, BanSnapshotHolder banSnapshotHolder) {
    return new BanSnapshotRefresher(accessControlStore, banSnapshotHolder, Clock.systemUTC());
  }

  @Bean
  @ConditionalOnMissingBean(AccessControlService.class)
  public AccessControlService accessControlService(
      AccessControlStore accessControlStore,
      AccessControlProperties properties,
      AccessControlMetrics accessControlMetrics,
      BanSnapshotHolder banSnapshotHolder) {
    return new AccessControlService(
        accessControlStore, properties, accessControlMetrics, banSnapshotHolder, Clock.systemUTC());
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

  static LettuceConnectionFactory createDedicatedConnectionFactory(
      RedisConnectionFactory redisConnectionFactory, AccessControlProperties properties) {
    Duration commandTimeout = resolveCommandTimeout(properties);
    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .clientOptions(
                ClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(commandTimeout).build())
                    .build())
            .build();

    if (!(redisConnectionFactory instanceof LettuceConnectionFactory source)) {
      throw new IllegalStateException(
          "access-control requires LettuceConnectionFactory; got "
              + redisConnectionFactory.getClass().getName());
    }

    RedisClusterConfiguration clusterConfiguration = source.getClusterConfiguration();
    if (clusterConfiguration != null
        && clusterConfiguration.getClusterNodes() != null
        && !clusterConfiguration.getClusterNodes().isEmpty()) {
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(clusterConfiguration, clientConfig);
      factory.setShareNativeConnection(source.getShareNativeConnection());
      return factory;
    }

    RedisStandaloneConfiguration standaloneConfiguration = source.getStandaloneConfiguration();
    if (standaloneConfiguration == null) {
      throw new IllegalStateException(
          "access-control could not resolve Redis standalone/cluster configuration");
    }
    LettuceConnectionFactory factory =
        new LettuceConnectionFactory(standaloneConfiguration, clientConfig);
    factory.setShareNativeConnection(source.getShareNativeConnection());
    factory.setDatabase(source.getDatabase());
    return factory;
  }

  private static Duration resolveCommandTimeout(AccessControlProperties properties) {
    Duration configured = properties.getRedisCommandTimeout();
    if (configured == null || configured.isZero() || configured.isNegative()) {
      return Duration.ofMillis(200);
    }
    return configured;
  }
}
