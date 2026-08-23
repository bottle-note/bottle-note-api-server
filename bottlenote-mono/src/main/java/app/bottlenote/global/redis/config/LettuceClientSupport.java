package app.bottlenote.global.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.resource.ClientResources;
import java.time.Duration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/** 공용 Lettuce 소켓 복원력. 전용 factory는 command timeout만 다르게 두고 이 정책을 재사용한다. */
public final class LettuceClientSupport {

  public static final Duration KEEP_ALIVE_IDLE = Duration.ofSeconds(60);
  public static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(10);
  public static final int KEEP_ALIVE_COUNT = 3;
  public static final int REQUEST_QUEUE_SIZE = 2048;

  private LettuceClientSupport() {}

  public static SocketOptions socketOptions(Duration connectTimeout) {
    return SocketOptions.builder()
        .connectTimeout(connectTimeout)
        .keepAlive(
            SocketOptions.KeepAliveOptions.builder()
                .enable()
                .idle(KEEP_ALIVE_IDLE)
                .interval(KEEP_ALIVE_INTERVAL)
                .count(KEEP_ALIVE_COUNT)
                .build())
        .build();
  }

  public static ClientOptions clientOptions(Duration connectTimeout) {
    return ClientOptions.builder()
        .autoReconnect(true)
        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
        .requestQueueSize(REQUEST_QUEUE_SIZE)
        .socketOptions(socketOptions(connectTimeout))
        .build();
  }

  public static LettuceClientConfiguration clientConfiguration(Duration commandTimeout) {
    return clientConfiguration(commandTimeout, null);
  }

  public static LettuceClientConfiguration clientConfiguration(
      Duration commandTimeout, ClientResources clientResources) {
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
        LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .clientOptions(clientOptions(commandTimeout));
    if (clientResources != null) {
      builder.clientResources(clientResources);
    }
    return builder.build();
  }

  public static LettuceConnectionFactory dedicatedFactory(
      RedisConnectionFactory redisConnectionFactory, Duration commandTimeout) {
    if (!(redisConnectionFactory instanceof LettuceConnectionFactory source)) {
      throw new IllegalStateException(
          "LettuceConnectionFactory가 필요합니다. 실제 타입: " + redisConnectionFactory.getClass().getName());
    }
    LettuceClientConfiguration clientConfig =
        clientConfiguration(commandTimeout, sourceClientResources(source));

    RedisClusterConfiguration clusterConfiguration = source.getClusterConfiguration();
    if (clusterConfiguration != null
        && clusterConfiguration.getClusterNodes() != null
        && !clusterConfiguration.getClusterNodes().isEmpty()) {
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(clusterConfiguration, clientConfig);
      applySharedConnectionPolicy(factory);
      return factory;
    }

    RedisStandaloneConfiguration standaloneConfiguration = source.getStandaloneConfiguration();
    if (standaloneConfiguration == null) {
      throw new IllegalStateException("Redis standalone/cluster 설정을 해석할 수 없습니다.");
    }
    LettuceConnectionFactory factory =
        new LettuceConnectionFactory(standaloneConfiguration, clientConfig);
    applySharedConnectionPolicy(factory);
    factory.setDatabase(source.getDatabase());
    return factory;
  }

  public static void applySharedConnectionPolicy(LettuceConnectionFactory factory) {
    factory.setShareNativeConnection(true);
    factory.setValidateConnection(false);
  }

  public static void start(LettuceConnectionFactory factory) {
    factory.afterPropertiesSet();
    factory.start();
  }

  private static ClientResources sourceClientResources(LettuceConnectionFactory source) {
    ClientResources configuredResources = source.getClientResources();
    if (configuredResources != null) {
      return configuredResources;
    }
    if (source.getNativeClient() != null) {
      return source.getNativeClient().getResources();
    }
    throw new IllegalStateException("Lettuce ClientResources를 확인할 수 없습니다.");
  }
}
