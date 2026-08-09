package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Tag("unit")
@DisplayName("AccessControlConfiguration Bean 배선 테스트")
class AccessControlConfigurationTest {

  private final ApplicationContextRunner enabledContextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              SharedRedisTestConfig.class,
              UnqualifiedRedisConsumersConfig.class,
              AccessControlConfiguration.class)
          .withPropertyValues(
              "bottlenote.access-control.enabled=true",
              "bottlenote.access-control.redis-command-timeout=200ms",
              "bottlenote.access-control.snapshot.refresh-interval-ms=45000",
              "bottlenote.access-control.snapshot.stale-threshold=4m",
              "bottlenote.access-control.snapshot.max-entries=2",
              "bottlenote.access-control.rate-limit-connection-count=3",
              "bottlenote.access-control.burst-admission.max-concurrent=7",
              "bottlenote.access-control.burst-admission.cooldown=2s");

  @Test
  @DisplayName("access-control 활성 시 RedisConnectionFactory·StringRedisTemplate 타입 후보가 각각 하나다")
  void redisTypeCandidatesRemainSingleWithoutPrimary() {
    enabledContextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          Map<String, RedisConnectionFactory> factories =
              context.getBeansOfType(RedisConnectionFactory.class);
          Map<String, StringRedisTemplate> templates =
              context.getBeansOfType(StringRedisTemplate.class);

          assertThat(factories).hasSize(1);
          assertThat(templates).hasSize(1);
          assertThat(context).hasSingleBean(RedisConnectionFactory.class);
          assertThat(context).hasSingleBean(StringRedisTemplate.class);
          assertThat(context).hasSingleBean(BanSnapshotHolder.class);
          assertThat(context).hasSingleBean(BanSnapshotRefresher.class);
          AccessControlProperties.Snapshot snapshotProperties =
              context.getBean(AccessControlProperties.class).getSnapshot();
          assertThat(snapshotProperties.getRefreshIntervalMs()).isEqualTo(45_000L);
          assertThat(snapshotProperties.getStaleThreshold()).isEqualTo(Duration.ofMinutes(4));
          assertThat(snapshotProperties.getMaxEntries()).isEqualTo(2);
          assertThat(context.getBean(AccessControlProperties.class).getRateLimitConnectionCount())
              .isEqualTo(3);
          AccessControlProperties.BurstAdmission admissionProperties =
              context.getBean(AccessControlProperties.class).getBurstAdmission();
          assertThat(admissionProperties.getMaxConcurrent()).isEqualTo(7);
          assertThat(admissionProperties.getCooldown()).isEqualTo(Duration.ofSeconds(2));

          // 무수식 주입 — @Primary 없이도 성공해야 한다
          RedisConnectionFactoryFactoryProbe factoryProbe =
              context.getBean(RedisConnectionFactoryFactoryProbe.class);
          StringRedisTemplateProbe templateProbe = context.getBean(StringRedisTemplateProbe.class);
          RedisTemplateProbe redisTemplateProbe = context.getBean(RedisTemplateProbe.class);

          RedisConnectionFactory sharedFactory = context.getBean(RedisConnectionFactory.class);
          assertThat(factoryProbe.connectionFactory()).isSameAs(sharedFactory);
          assertThat(templateProbe.stringRedisTemplate().getConnectionFactory())
              .isSameAs(sharedFactory);
          assertThat(redisTemplateProbe.redisTemplate().getConnectionFactory())
              .isSameAs(sharedFactory);
        });
  }

  @Test
  @DisplayName("AccessControlStore는 비등록 전용 factory(200ms)를 소유하고 공용 factory와 다르다")
  void accessControlStore_ownsDedicatedNonBeanFactoryWith200msTimeout() {
    enabledContextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          AccessControlStore store = context.getBean(AccessControlStore.class);
          assertThat(store).isInstanceOf(RedisAccessControlStore.class);
          RedisAccessControlStore redisStore = (RedisAccessControlStore) store;

          StringRedisTemplate storeTemplate = extractTemplate(redisStore);
          LettuceConnectionFactory ownedFactory = extractOwnedFactory(redisStore);
          List<?> rateLimitTemplates = extractRateLimitTemplates(redisStore);
          List<?> ownedFactories = extractOwnedFactories(redisStore);
          RedisConnectionFactory sharedFactory = context.getBean(RedisConnectionFactory.class);
          LettuceConnectionFactory sharedLettuceFactory = (LettuceConnectionFactory) sharedFactory;
          Object sharedClientResources = sharedClientResources(sharedLettuceFactory);

          assertThat(ownedFactory).isNotNull();
          assertThat(ownedFactory).isNotSameAs(sharedFactory);
          assertThat(storeTemplate.getConnectionFactory()).isSameAs(ownedFactory);
          assertThat(ownedFactory.getClientConfiguration().getCommandTimeout())
              .isEqualTo(Duration.ofMillis(200));
          assertThat(rateLimitTemplates).hasSize(3);
          assertThat(ownedFactories).hasSize(4);
          assertThat(rateLimitTemplates)
              .allSatisfy(template -> assertThat(template).isInstanceOf(StringRedisTemplate.class));
          assertThat(ownedFactories)
              .allSatisfy(
                  candidate -> {
                    assertThat(candidate).isInstanceOf(LettuceConnectionFactory.class);
                    LettuceConnectionFactory factory = (LettuceConnectionFactory) candidate;
                    assertThat(factory.getClientConfiguration().getCommandTimeout())
                        .isEqualTo(Duration.ofMillis(200));
                    assertThat(factory.getClientResources()).isSameAs(sharedClientResources);
                    assertThat(factory.getShareNativeConnection()).isTrue();
                  });

          // 전용 factory/template은 Spring 타입 후보에 포함되지 않는다
          assertThat(context.getBeansOfType(RedisConnectionFactory.class).values())
              .containsExactly(sharedFactory)
              .doesNotContain(ownedFactory);
          assertThat(context.getBeansOfType(StringRedisTemplate.class).values())
              .doesNotContain(storeTemplate);
          assertThat(context.getBean(StringRedisTemplate.class).getConnectionFactory())
              .isSameAs(sharedFactory);
        });
  }

  @Test
  @DisplayName("access-control 비활성 시 store를 만들지 않는다")
  void disabled_doesNotCreateAccessControlBeans() {
    new ApplicationContextRunner()
        .withUserConfiguration(SharedRedisTestConfig.class, AccessControlConfiguration.class)
        .withPropertyValues("bottlenote.access-control.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(AccessControlStore.class);
              assertThat(context).doesNotHaveBean(AccessControlService.class);
              assertThat(context).doesNotHaveBean(BanSnapshotHolder.class);
              assertThat(context).doesNotHaveBean(BanSnapshotRefresher.class);
              assertThat(context.getBeansOfType(RedisConnectionFactory.class)).hasSize(1);
            });
  }

  @Test
  @DisplayName("rate-limit 전용 연결 수는 1부터 8까지만 허용한다")
  void rateLimitConnectionCount_whenOutsideRange_failsContextCreation() {
    enabledContextRunner
        .withPropertyValues("bottlenote.access-control.rate-limit-connection-count=0")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalArgumentException.class)
                  .hasRootCauseMessage(
                      "access-control.rateLimitConnectionCount must be between 1 and 8");
            });
  }

  private static StringRedisTemplate extractTemplate(RedisAccessControlStore store) {
    try {
      Field field = RedisAccessControlStore.class.getDeclaredField("redisTemplate");
      field.setAccessible(true);
      return (StringRedisTemplate) field.get(store);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static LettuceConnectionFactory extractOwnedFactory(RedisAccessControlStore store) {
    return (LettuceConnectionFactory) extractTemplate(store).getConnectionFactory();
  }

  private static List<?> extractRateLimitTemplates(RedisAccessControlStore store) {
    try {
      Field field = RedisAccessControlStore.class.getDeclaredField("rateLimitTemplates");
      field.setAccessible(true);
      return (List<?>) field.get(store);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static List<?> extractOwnedFactories(RedisAccessControlStore store) {
    try {
      Field field = RedisAccessControlStore.class.getDeclaredField("ownedConnectionFactories");
      field.setAccessible(true);
      return (List<?>) field.get(store);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static Object sharedClientResources(LettuceConnectionFactory factory) {
    if (factory.getClientResources() != null) {
      return factory.getClientResources();
    }
    return factory.getNativeClient().getResources();
  }

  /** 실제 RedisConfig와 같이 @Primary 없는 단일 RedisConnectionFactory. */
  @Configuration
  static class SharedRedisTestConfig {

    @Bean
    LettuceConnectionFactory redisConnectionFactory() {
      LettuceClientConfiguration clientConfig =
          LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(15)).build();
      return new LettuceConnectionFactory(
          new RedisStandaloneConfiguration("127.0.0.1", 6379), clientConfig);
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
      RedisTemplate<String, Object> template = new RedisTemplate<>();
      template.setConnectionFactory(connectionFactory);
      template.setKeySerializer(new StringRedisSerializer());
      template.setValueSerializer(new StringRedisSerializer());
      return template;
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @Configuration
  static class UnqualifiedRedisConsumersConfig {

    @Bean
    RedisConnectionFactoryFactoryProbe factoryProbe(
        @Autowired RedisConnectionFactory connectionFactory) {
      return new RedisConnectionFactoryFactoryProbe(connectionFactory);
    }

    @Bean
    StringRedisTemplateProbe stringRedisTemplateProbe(
        @Autowired StringRedisTemplate stringRedisTemplate) {
      return new StringRedisTemplateProbe(stringRedisTemplate);
    }

    @Bean
    RedisTemplateProbe redisTemplateProbe(@Autowired RedisTemplate<String, Object> redisTemplate) {
      return new RedisTemplateProbe(redisTemplate);
    }
  }

  record RedisConnectionFactoryFactoryProbe(RedisConnectionFactory connectionFactory) {}

  record StringRedisTemplateProbe(StringRedisTemplate stringRedisTemplate) {}

  record RedisTemplateProbe(RedisTemplate<String, Object> redisTemplate) {}
}
