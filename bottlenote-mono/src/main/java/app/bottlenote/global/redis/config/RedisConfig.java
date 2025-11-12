package app.bottlenote.global.redis.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** Redis 설정 클래스 (Spring Boot Auto-configuration 사용) */
@Slf4j
@Configuration
@EnableRedisRepositories(basePackages = "app.bottlenote.global.redis.repository")
@RequiredArgsConstructor
public class RedisConfig {

  private final RedisConnectionDetails redisConnectionDetails;

  /** 애플리케이션 시작 시 Redis 설정 정보를 로그로 출력합니다. */
  @PostConstruct
  public void init() {
    if (redisConnectionDetails.getCluster() != null) {
      log.info("Redis 연결 모드: 클러스터 모드");
      log.info("클러스터 노드: {}", redisConnectionDetails.getCluster().getNodes());
    } else {
      log.info("Redis 연결 모드: 단일 노드 모드");
      RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();
      log.info("단일 노드 연결: {}:{}", standalone.getHost(), standalone.getPort());
    }
    log.info("Redis 연결 설정이 완료되었습니다.");
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
