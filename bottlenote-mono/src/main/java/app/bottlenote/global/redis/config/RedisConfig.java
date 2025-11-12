package app.bottlenote.global.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  private final RedisProperties redisProperties;

  @Value("${spring.data.redis.mode:standalone}")
  private String redisMode;

  /** 애플리케이션 시작 시 Redis 설정 정보를 로그로 출력합니다. */
  @PostConstruct
  public void init() {
    log.info("Redis 연결 모드: {}", redisMode);

    switch (redisMode.toLowerCase()) {
      case "cluster" -> {
        RedisConnectionDetails.Cluster cluster = redisConnectionDetails.getCluster();
        if (cluster != null && cluster.getNodes() != null) {
          log.info("클러스터 노드: {}", cluster.getNodes());
        }
      }
      default -> {
        RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();
        if (standalone != null) {
          log.info("단일 노드 연결: {}:{}", standalone.getHost(), standalone.getPort());
        }
      }
    }

    log.info("Redis 연결 설정이 완료되었습니다.");
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
      default -> createStandaloneConnectionFactory(clientConfig);
    };
  }

  /** Lettuce 클라이언트 설정 생성 */
  private LettuceClientConfiguration createLettuceClientConfiguration() {
    SocketOptions socketOptions =
        SocketOptions.builder()
            .connectTimeout(
                Duration.ofSeconds(
                    redisProperties.getTimeout() != null
                        ? redisProperties.getTimeout().getSeconds()
                        : 15))
            .build();

    ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();

    return LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .commandTimeout(
            Duration.ofSeconds(
                redisProperties.getTimeout() != null
                    ? redisProperties.getTimeout().getSeconds()
                    : 15))
        .build();
  }

  /** Standalone 모드 ConnectionFactory 생성 */
  private LettuceConnectionFactory createStandaloneConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();

    if (standalone == null) {
      throw new IllegalStateException("Standalone 모드에서 RedisConnectionDetails.Standalone이 null입니다.");
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
    cluster.getNodes().forEach(node ->
        clusterConfig.clusterNode(node.host(), node.port())
    );

    if (StringUtils.hasText(redisConnectionDetails.getPassword())) {
      clusterConfig.setPassword(redisConnectionDetails.getPassword());
    }

    if (StringUtils.hasText(redisConnectionDetails.getUsername())) {
      clusterConfig.setUsername(redisConnectionDetails.getUsername());
    }

    return new LettuceConnectionFactory(clusterConfig, clientConfig);
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
