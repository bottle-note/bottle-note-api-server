package app.bottlenote.global.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

/** Redis 설정 클래스 - Standalone/Cluster/Sentinel 모드 동적 지원 */
@Slf4j
@Configuration
@EnableRedisRepositories(basePackages = "app.bottlenote.global.redis.repository")
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisProperties redisProperties;

  @Value("${spring.data.redis.mode:standalone}")
  private String redisMode;

  /** 애플리케이션 시작 시 Redis 설정 정보를 로그로 출력합니다. */
  @PostConstruct
  public void init() {
    log.info("Redis 연결 모드: {}", redisMode);

    switch (redisMode.toLowerCase()) {
      case "cluster" -> {
        if (redisProperties.getCluster() != null && redisProperties.getCluster().getNodes() != null) {
          log.info("클러스터 노드: {}", redisProperties.getCluster().getNodes());
        }
      }
      case "sentinel" -> {
        if (redisProperties.getSentinel() != null) {
          log.info("센티넬 마스터: {}", redisProperties.getSentinel().getMaster());
          log.info("센티넬 노드: {}", redisProperties.getSentinel().getNodes());
        }
      }
      default -> log.info("단일 노드 연결: {}:{}", redisProperties.getHost(), redisProperties.getPort());
    }

    log.info("Redis 연결 설정이 완료되었습니다.");
  }

  /**
   * Redis 연결 팩토리를 생성합니다. (모드에 따라 Standalone/Cluster/Sentinel 동적 생성)
   *
   * @return RedisConnectionFactory
   */
  @Bean
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
    SocketOptions socketOptions = SocketOptions.builder()
        .connectTimeout(Duration.ofSeconds(redisProperties.getTimeout() != null
            ? redisProperties.getTimeout().getSeconds()
            : 15))
        .build();

    ClientOptions clientOptions = ClientOptions.builder()
        .socketOptions(socketOptions)
        .build();

    return LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .commandTimeout(Duration.ofSeconds(redisProperties.getTimeout() != null
            ? redisProperties.getTimeout().getSeconds()
            : 15))
        .build();
  }

  /** Standalone 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createStandaloneConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName(redisProperties.getHost());
    standaloneConfig.setPort(redisProperties.getPort());

    if (StringUtils.hasText(redisProperties.getPassword())) {
      standaloneConfig.setPassword(redisProperties.getPassword());
    }

    return new LettuceConnectionFactory(standaloneConfig, clientConfig);
  }

  /** Cluster 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createClusterConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisProperties.Cluster clusterProperties = redisProperties.getCluster();

    if (clusterProperties == null || clusterProperties.getNodes() == null || clusterProperties.getNodes().isEmpty()) {
      throw new IllegalArgumentException("Redis Cluster 모드에서 nodes 설정이 필요합니다.");
    }

    RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterProperties.getNodes());

    if (StringUtils.hasText(redisProperties.getPassword())) {
      clusterConfig.setPassword(redisProperties.getPassword());
    }

    return new LettuceConnectionFactory(clusterConfig, clientConfig);
  }

  /** Sentinel 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createSentinelConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisProperties.Sentinel sentinelProperties = redisProperties.getSentinel();

    if (sentinelProperties == null || !StringUtils.hasText(sentinelProperties.getMaster())
        || sentinelProperties.getNodes() == null || sentinelProperties.getNodes().isEmpty()) {
      throw new IllegalArgumentException("Redis Sentinel 모드에서 master와 nodes 설정이 필요합니다.");
    }

    RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration(
        sentinelProperties.getMaster(),
        sentinelProperties.getNodes());

    if (StringUtils.hasText(redisProperties.getPassword())) {
      sentinelConfig.setPassword(redisProperties.getPassword());
    }

    return new LettuceConnectionFactory(sentinelConfig, clientConfig);
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
