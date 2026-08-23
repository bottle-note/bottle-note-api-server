package app.batch.bottlenote.config;

import app.bottlenote.global.redis.config.LettuceClientSupport;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.data.redis.sentinel.master")
public class BatchRedisSentinelConfiguration {

  @Bean
  LettuceClientConfigurationBuilderCustomizer batchSentinelTopologyCustomizer() {
    return builder -> builder.readFrom(ReadFrom.MASTER);
  }

  @Bean
  LettuceClientOptionsBuilderCustomizer batchSentinelClientOptionsCustomizer(
      RedisProperties redisProperties) {
    Duration connectTimeout = redisProperties.getConnectTimeout();
    if (connectTimeout == null) {
      connectTimeout = redisProperties.getTimeout();
    }
    if (connectTimeout == null) {
      connectTimeout = SocketOptions.DEFAULT_CONNECT_TIMEOUT_DURATION;
    }
    Duration resolvedConnectTimeout = connectTimeout;
    return builder ->
        LettuceClientSupport.configureClientOptions(builder, resolvedConnectTimeout);
  }
}
