package app.bottlenote.global.redis.config;

import app.bottlenote.observability.visitor.RedisVisitorIdentityResolver;
import app.bottlenote.observability.visitor.VisitorIdentityResolver;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "bottlenote.observability.visitor-telemetry",
    name = "enabled",
    havingValue = "true")
public class VisitorIdentityConfiguration {
  private LettuceConnectionFactory ownedFactory;

  @Bean
  VisitorIdentityResolver visitorIdentityResolver(RedisConnectionFactory connectionFactory) {
    ownedFactory = LettuceClientSupport.dedicatedFactory(connectionFactory, Duration.ofMillis(200));
    try {
      LettuceClientSupport.start(ownedFactory);
      return new RedisVisitorIdentityResolver(new StringRedisTemplate(ownedFactory));
    } catch (RuntimeException exception) {
      destroy();
      throw exception;
    }
  }

  @PreDestroy
  void destroy() {
    if (ownedFactory != null) {
      ownedFactory.destroy();
      ownedFactory = null;
    }
  }
}
