package app.batch.bottlenote.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Tag("unit")
@DisplayName("Batch Redis Sentinel 연결 설정 테스트")
class BatchRedisSentinelConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(BatchRedisSentinelConfiguration.class)
          .withBean(RedisProperties.class, BatchRedisSentinelConfigurationTest::redisProperties);

  @Test
  @DisplayName("Sentinel 설정이 있으면 master topology와 공용 소켓 복원력을 적용한다")
  void sentinelProperties_applyMasterTopologyAndSocketResilience() {
    contextRunner
        .withPropertyValues("spring.data.redis.sentinel.master=bottlenote-master")
        .run(
            context -> {
              LettuceClientConfiguration.LettuceClientConfigurationBuilder configurationBuilder =
                  LettuceClientConfiguration.builder();
              context
                  .getBean(LettuceClientConfigurationBuilderCustomizer.class)
                  .customize(configurationBuilder);

              ClientOptions.Builder optionsBuilder = ClientOptions.builder();
              context
                  .getBean(LettuceClientOptionsBuilderCustomizer.class)
                  .customize(optionsBuilder);
              ClientOptions options = optionsBuilder.build();

              assertThat(configurationBuilder.build().getReadFrom()).contains(ReadFrom.MASTER);
              assertThat(options.isAutoReconnect()).isTrue();
              assertThat(options.getDisconnectedBehavior())
                  .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
              assertThat(options.getRequestQueueSize()).isEqualTo(2048);
              assertThat(options.getSocketOptions().getConnectTimeout())
                  .isEqualTo(Duration.ofSeconds(3));
              assertThat(options.getSocketOptions().getKeepAlive().isEnabled()).isTrue();
              assertThat(options.getSocketOptions().getTcpUserTimeout().isEnabled()).isTrue();
            });
  }

  @Test
  @DisplayName("Spring Boot가 생성한 Sentinel factory에도 topology와 소켓 정책을 적용한다")
  void redisAutoConfiguration_appliesSentinelCustomizersToConnectionFactory() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
        .withUserConfiguration(BatchRedisSentinelConfiguration.class)
        .withPropertyValues(
            "spring.data.redis.sentinel.master=bottlenote-master",
            "spring.data.redis.sentinel.nodes=127.0.0.1:26379",
            "spring.data.redis.timeout=15s",
            "spring.data.redis.connect-timeout=3s")
        .run(
            context -> {
              LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
              ClientOptions options = factory.getClientConfiguration().getClientOptions().orElseThrow();

              assertThat(factory.getSentinelConfiguration()).isNotNull();
              assertThat(factory.getSentinelConfiguration().getMaster().getName())
                  .isEqualTo("bottlenote-master");
              assertThat(factory.getClientConfiguration().getReadFrom()).contains(ReadFrom.MASTER);
              assertThat(options.getSocketOptions().getConnectTimeout())
                  .isEqualTo(Duration.ofSeconds(3));
              assertThat(options.getSocketOptions().getKeepAlive().isEnabled()).isTrue();
              assertThat(options.getTimeoutOptions().isTimeoutCommands()).isTrue();
            });
  }

  @Test
  @DisplayName("Sentinel 설정이 없으면 Batch 전용 customizer를 등록하지 않는다")
  void standaloneProperties_doNotRegisterSentinelCustomizers() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(LettuceClientConfigurationBuilderCustomizer.class);
          assertThat(context).doesNotHaveBean(LettuceClientOptionsBuilderCustomizer.class);
        });
  }

  @Test
  @DisplayName("Redis timeout이 없으면 Lettuce 기본 connect timeout을 유지한다")
  void missingRedisTimeout_usesLettuceDefaultConnectTimeout() {
    RedisProperties properties = new RedisProperties();
    ClientOptions.Builder optionsBuilder = ClientOptions.builder();

    new BatchRedisSentinelConfiguration()
        .batchSentinelClientOptionsCustomizer(properties)
        .customize(optionsBuilder);

    assertThat(optionsBuilder.build().getSocketOptions().getConnectTimeout())
        .isEqualTo(SocketOptions.DEFAULT_CONNECT_TIMEOUT_DURATION);
  }

  private static RedisProperties redisProperties() {
    RedisProperties properties = new RedisProperties();
    properties.setTimeout(Duration.ofSeconds(15));
    properties.setConnectTimeout(Duration.ofSeconds(3));
    return properties;
  }
}
