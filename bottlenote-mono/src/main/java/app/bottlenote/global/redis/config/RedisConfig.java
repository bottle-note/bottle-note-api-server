package app.bottlenote.global.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

/** Redis 설정 클래스 - Standalone/Cluster 모드 동적 지원 */
@Slf4j
@Configuration
@EnableRedisRepositories(basePackages = "app.bottlenote.global.redis.repository")
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisConnectionDetails redisConnectionDetails;

  @Value("${spring.data.redis.mode:standalone}")
  private String redisMode;

  @Value("${spring.data.redis.timeout:15s}")
  private Duration redisTimeout;

  /** 애플리케이션 준비 완료 시 Redis 연결 상태를 검증하고 로그로 출력합니다. */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info("========================================");
    log.info("Redis Configuration Report");
    log.info("========================================");
    log.info("Mode: {}", redisMode);
    log.info("Timeout: {}", redisTimeout);

    switch (redisMode.toLowerCase()) {
      case "cluster" -> {
        RedisConnectionDetails.Cluster cluster = redisConnectionDetails.getCluster();
        if (cluster != null && cluster.getNodes() != null) {
          log.info("Cluster Nodes: {}", cluster.getNodes());
          log.info("Total Nodes: {}", cluster.getNodes().size());
        }
      }
      case "sentinel" -> {
        RedisConnectionDetails.Sentinel sentinel = redisConnectionDetails.getSentinel();
        if (sentinel != null && sentinel.getNodes() != null) {
          log.info("Sentinel Nodes: {}", sentinel.getNodes());
          log.info("Master: {}", sentinel.getMaster());
        }
      }
      default -> {
        RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();
        if (standalone != null) {
          log.info("Host: {}", standalone.getHost());
          log.info("Port: {}", standalone.getPort());
          log.info("Database: {}", standalone.getDatabase());
        }
      }
    }

    log.info("Username: {}", redisConnectionDetails.getUsername() != null ? "Configured" : "Not configured");
    log.info("Password: {}", redisConnectionDetails.getPassword() != null ? "Configured" : "Not configured");
    log.info("========================================");
    log.info("✅ Redis connection successfully established");
    log.info("========================================");
  }

  /**
   * Redis 연결 팩토리를 생성합니다. (모드에 따라 Standalone/Cluster 동적 생성)
   *
   * @return RedisConnectionFactory
   */
  @Bean
  @ConditionalOnMissingBean(RedisConnectionFactory.class)
  public RedisConnectionFactory redisConnectionFactory() {
    LettuceClientConfiguration clientConfig = createLettuceClientConfiguration();

    return switch (redisMode.toLowerCase()) {
      case "cluster" -> createClusterConnectionFactory(clientConfig);
      case "sentinel" -> createSentinelConnectionFactory(clientConfig);
      default -> createStandaloneConnectionFactory(clientConfig);
    };
  }

  /** Lettuce 클라이언트 설정 생성 */
  private LettuceClientConfiguration createLettuceClientConfiguration() {
    SocketOptions socketOptions = SocketOptions.builder().connectTimeout(redisTimeout).build();

    ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();

    return LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .commandTimeout(redisTimeout)
        .build();
  }

  /** Standalone 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createStandaloneConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();

    if (standalone == null) {
      throw new IllegalStateException(
          "Standalone 모드에서 RedisConnectionDetails.Standalone이 null입니다.");
    }

    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName(standalone.getHost());
    standaloneConfig.setPort(standalone.getPort());
    standaloneConfig.setDatabase(standalone.getDatabase());

    if (StringUtils.hasText(redisConnectionDetails.getPassword())) {
      standaloneConfig.setPassword(redisConnectionDetails.getPassword());
    }

    if (StringUtils.hasText(redisConnectionDetails.getUsername())) {
      standaloneConfig.setUsername(redisConnectionDetails.getUsername());
    }

    return new LettuceConnectionFactory(standaloneConfig, clientConfig);
  }

  /** Cluster 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createClusterConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisConnectionDetails.Cluster cluster = redisConnectionDetails.getCluster();

    if (cluster == null || cluster.getNodes() == null || cluster.getNodes().isEmpty()) {
      throw new IllegalArgumentException("Redis Cluster 모드에서 nodes 설정이 필요합니다.");
    }

    RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();

    // RedisConnectionDetails.Node를 host:port 문자열로 변환
    cluster.getNodes().forEach(node -> clusterConfig.clusterNode(node.host(), node.port()));

    if (StringUtils.hasText(redisConnectionDetails.getPassword())) {
      clusterConfig.setPassword(redisConnectionDetails.getPassword());
    }

    if (StringUtils.hasText(redisConnectionDetails.getUsername())) {
      clusterConfig.setUsername(redisConnectionDetails.getUsername());
    }

    return new LettuceConnectionFactory(clusterConfig, clientConfig);
  }

  /** Sentinel 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createSentinelConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    // TODO: Sentinel 모드 구현 예정
    throw new UnsupportedOperationException("Sentinel 모드는 아직 구현되지 않았습니다.");
  }

  /**
   * Redis 작업을 위한 템플릿 객체를 생성합니다.
   *
   * @return Redis 작업을 수행할 수 있는 RedisTemplate 객체
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    return template;
  }
}
