package app.bottlenote.global.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.resource.ClientResources;
import java.time.Duration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/** 공용 Lettuce 소켓 복원력. 전용 factory는 command timeout만 다르게 두고 이 정책을 재사용한다. */
public final class LettuceClientSupport {

  public static final Duration KEEP_ALIVE_IDLE = Duration.ofSeconds(60);
  public static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(10);
  public static final int KEEP_ALIVE_COUNT = 3;
  public static final int REQUEST_QUEUE_SIZE = 2048;
  public static final Duration TCP_USER_TIMEOUT = Duration.ofSeconds(30);

  private static final String EPOLL_CLASS = "io.netty.channel.epoll.Epoll";

  private LettuceClientSupport() {}

  /**
   * keepalive는 연결이 유휴일 때만 프로브를 보내므로, 미확인 데이터가 쌓인 채 끊긴 반열림 연결은 감지하지 못한다. 그 경우 커널 재전송(tcp_retries2)이
   * 만료될 때까지 15~30분이 걸린다. TCP_USER_TIMEOUT이 그 상한을 30초로 끊는다.
   *
   * <p>단 이 옵션은 리눅스 epoll 트랜스포트에서만 적용된다. epoll이 없으면 Lettuce가 경고만 남기고 무시하므로 기동에는 영향이 없다.
   */
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
        .tcpUserTimeout(
            SocketOptions.TcpUserTimeoutOptions.builder()
                .enable()
                .tcpUserTimeout(TCP_USER_TIMEOUT)
                .build())
        .build();
  }

  /**
   * epoll 네이티브 트랜스포트 가용 여부. 클래스 자체가 runtimeOnly라 컴파일 시점에는 참조할 수 없으므로 리플렉션으로 확인한다. 반환값이 false면
   * TCP_USER_TIMEOUT은 적용되지 않는다.
   */
  public static boolean isEpollAvailable() {
    try {
      Class<?> epoll = Class.forName(EPOLL_CLASS);
      return Boolean.TRUE.equals(epoll.getMethod("isAvailable").invoke(null));
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
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
    return clientConfiguration(commandTimeout, commandTimeout, clientResources);
  }

  public static LettuceClientConfiguration clientConfiguration(
      Duration commandTimeout, Duration connectTimeout, ClientResources clientResources) {
    return clientConfiguration(commandTimeout, connectTimeout, clientResources, null);
  }

  public static LettuceClientConfiguration sentinelClientConfiguration(Duration timeout) {
    return clientConfiguration(timeout, timeout, null, ReadFrom.MASTER);
  }

  private static LettuceClientConfiguration clientConfiguration(
      Duration commandTimeout,
      Duration connectTimeout,
      ClientResources clientResources,
      ReadFrom readFrom) {
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
        LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .clientOptions(clientOptions(connectTimeout));
    if (clientResources != null) {
      builder.clientResources(clientResources);
    }
    if (readFrom != null) {
      builder.readFrom(readFrom);
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
        clientConfiguration(
            commandTimeout,
            sourceConnectTimeout(source),
            sourceClientResources(source),
            source.getClientConfiguration().getReadFrom().orElse(null));

    RedisClusterConfiguration clusterConfiguration = source.getClusterConfiguration();
    if (clusterConfiguration != null
        && clusterConfiguration.getClusterNodes() != null
        && !clusterConfiguration.getClusterNodes().isEmpty()) {
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(clusterConfiguration, clientConfig);
      applySharedConnectionPolicy(factory);
      return factory;
    }

    RedisSentinelConfiguration sentinelConfiguration = source.getSentinelConfiguration();
    if (sentinelConfiguration != null
        && sentinelConfiguration.getMaster() != null
        && sentinelConfiguration.getSentinels() != null
        && !sentinelConfiguration.getSentinels().isEmpty()) {
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(sentinelConfiguration, clientConfig);
      applySharedConnectionPolicy(factory);
      return factory;
    }

    RedisStandaloneConfiguration standaloneConfiguration = source.getStandaloneConfiguration();
    if (standaloneConfiguration == null) {
      throw new IllegalStateException("Redis standalone/sentinel/cluster 설정을 해석할 수 없습니다.");
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

  private static Duration sourceConnectTimeout(LettuceConnectionFactory source) {
    return source
        .getClientConfiguration()
        .getClientOptions()
        .map(ClientOptions::getSocketOptions)
        .map(SocketOptions::getConnectTimeout)
        .orElse(source.getClientConfiguration().getCommandTimeout());
  }
}
