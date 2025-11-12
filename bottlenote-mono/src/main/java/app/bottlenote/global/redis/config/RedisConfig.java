package app.bottlenote.global.redis.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
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

/** Redis 설정 클래스 Redis 연결 및 템플릿 구성을 관리합니다. 환경 설정에 따라 단일 노드 또는 클러스터 구성을 자동으로 선택합니다. */
@Slf4j
@Configuration
@EnableRedisRepositories(basePackages = "app.bottlenote.global.redis.repository")
public class RedisConfig {

  private final RedisProperties redisProperties;
  private final RedisConnectionDetails redisConnectionDetails;

  public RedisConfig(
      RedisProperties redisProperties, RedisConnectionDetails redisConnectionDetails) {
    this.redisProperties = redisProperties;
    this.redisConnectionDetails = redisConnectionDetails;
  }

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
   * Redis 연결 팩토리를 생성합니다. 환경 설정에 따라 단일 노드 또는 클러스터 연결을 자동으로 선택합니다.
   *
   * @return Redis 연결을 위한 ConnectionFactory 객체
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    // 클러스터 모드 여부 확인
    if (redisConnectionDetails.getCluster() != null) {
      log.info("Redis 클러스터 모드로 설정되었습니다.");
      return createClusterConnectionFactory();
    } else {
      log.info("Redis 단일 노드 모드로 설정되었습니다.");
      return createStandaloneConnectionFactory();
    }
  }

  /**
   * 단일 노드 Redis 연결 팩토리를 생성합니다.
   *
   * @return 단일 노드용 Redis 연결 팩토리
   */
  private RedisConnectionFactory createStandaloneConnectionFactory() {
    // RedisConnectionDetails에서 동적 설정 가져오기 (TestContainers 지원)
    RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();
    RedisStandaloneConfiguration redisConfig =
        new RedisStandaloneConfiguration(standalone.getHost(), standalone.getPort());

    // 비밀번호가 설정되어 있으면 적용
    if (standalone.getPassword() != null) {
      redisConfig.setPassword(standalone.getPassword());
    }

    // 데이터베이스 인덱스 설정
    redisConfig.setDatabase(standalone.getDatabase());

    // 클라이언트 구성 (타임아웃 설정 등)
    Duration timeout =
        redisProperties.getTimeout() != null
            ? redisProperties.getTimeout()
            : Duration.ofSeconds(15);
    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder().commandTimeout(timeout).build();

    // Lettuce 라이브러리를 사용하여 Redis에 연결하는 ConnectionFactory 생성
    return new LettuceConnectionFactory(redisConfig, clientConfig);
  }

  /**
   * 클러스터 모드 Redis 연결 팩토리를 생성합니다.
   *
   * @return 클러스터용 Redis 연결 팩토리
   */
  private RedisConnectionFactory createClusterConnectionFactory() {
    // RedisConnectionDetails에서 클러스터 설정 가져오기
    RedisConnectionDetails.Cluster cluster = redisConnectionDetails.getCluster();
    RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();

    // 클러스터 노드 설정
    cluster.getNodes().forEach(node -> clusterConfiguration.clusterNode(node.host(), node.port()));

    // 비밀번호 설정이 있으면 적용
    if (cluster.getPassword() != null) {
      clusterConfiguration.setPassword(cluster.getPassword());
    }

    // 클러스터 연결 시 타임아웃 설정 등 고급 옵션
    Duration timeout =
        redisProperties.getTimeout() != null
            ? redisProperties.getTimeout()
            : Duration.ofSeconds(15);
    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder()
            .commandTimeout(timeout)
            .shutdownTimeout(Duration.ofSeconds(2))
            .build();

    // Lettuce 클라이언트로 클러스터 연결 팩토리 생성
    return new LettuceConnectionFactory(clusterConfiguration, clientConfig);
  }

  /**
   * Redis 작업을 위한 템플릿 객체를 생성합니다. 단일 노드와 클러스터 모두 동일한 템플릿을 사용할 수 있습니다.
   *
   * @return Redis 작업을 수행할 수 있는 RedisTemplate 객체
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    // Redis 작업을 쉽게 수행할 수 있는 템플릿 객체 생성
    RedisTemplate<String, Object> template = new RedisTemplate<>();

    // 앞서 정의한 Redis 연결 팩토리 설정 (단일 또는 클러스터)
    template.setConnectionFactory(redisConnectionFactory());

    // 키 직렬화 방식 설정: 문자열 형식으로 직렬화
    template.setKeySerializer(new StringRedisSerializer());

    // 값 직렬화 방식 설정: JSON 형식으로 직렬화
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

    // 해시 작업을 위한 직렬화 설정
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

    return template;
  }
}
