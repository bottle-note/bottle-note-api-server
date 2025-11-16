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

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    validateRedisMode();

    log.info("========================================");
    log.info("Redis Configuration Report");
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

    log.info(
        "Username: {}",
        redisConnectionDetails.getUsername() != null ? "Configured" : "Not configured");
    log.info(
        "Password: {}",
        redisConnectionDetails.getPassword() != null ? "Configured" : "Not configured");
    log.info("✅ Redis connection successfully established");
    log.info("========================================");
  }

  private void validateRedisMode() {
    if ("sentinel".equalsIgnoreCase(redisMode)) {
      throw new UnsupportedOperationException(
          "Redis Sentinel mode is not yet supported. "
              + "Please use 'standalone' or 'cluster' mode. "
              + "Configure spring.data.redis.mode property accordingly.");
    }
  }

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

  private LettuceClientConfiguration createLettuceClientConfiguration() {
    SocketOptions socketOptions = SocketOptions.builder().connectTimeout(redisTimeout).build();

    ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();

    return LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .commandTimeout(redisTimeout)
        .build();
  }

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

  private LettuceConnectionFactory createClusterConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    RedisConnectionDetails.Cluster cluster = redisConnectionDetails.getCluster();

    if (cluster == null || cluster.getNodes() == null || cluster.getNodes().isEmpty()) {
      throw new IllegalArgumentException("Redis Cluster 모드에서 nodes 설정이 필요합니다.");
    }

    RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();

    cluster.getNodes().forEach(node -> clusterConfig.clusterNode(node.host(), node.port()));

    if (StringUtils.hasText(redisConnectionDetails.getPassword())) {
      clusterConfig.setPassword(redisConnectionDetails.getPassword());
    }

    if (StringUtils.hasText(redisConnectionDetails.getUsername())) {
      clusterConfig.setUsername(redisConnectionDetails.getUsername());
    }

    return new LettuceConnectionFactory(clusterConfig, clientConfig);
  }

  private LettuceConnectionFactory createSentinelConnectionFactory(
      LettuceClientConfiguration clientConfig) {
    throw new UnsupportedOperationException("Sentinel mode not yet implemented");
  }

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
