package app.bottlenote.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("Redis Sentinel 연결 설정 테스트")
class RedisConfigTest {

  @Test
  @DisplayName("Sentinel 탐색 인증과 Redis 데이터 노드 인증을 분리해 설정한다")
  void redisConnectionFactory_configuresSentinelAndDataNodeCredentialsSeparately() {
    RedisConnectionDetails connectionDetails =
        sentinelConnectionDetails(
            "data-user", "data-password", "sentinel-user", "sentinel-password");
    RedisConfig redisConfig = new RedisConfig(connectionDetails);
    ReflectionTestUtils.setField(redisConfig, "redisMode", "sentinel");
    ReflectionTestUtils.setField(redisConfig, "redisTimeout", Duration.ofSeconds(15));

    LettuceConnectionFactory factory =
        (LettuceConnectionFactory) redisConfig.redisConnectionFactory();
    RedisSentinelConfiguration sentinel = factory.getSentinelConfiguration();

    assertThat(sentinel).isNotNull();
    assertThat(sentinel.getMaster().getName()).isEqualTo("bottlenote-master");
    assertThat(sentinel.getSentinels())
        .extracting(node -> node.getHost() + ":" + node.getPort())
        .containsExactlyInAnyOrder("sentinel-0:26379", "sentinel-1:26379", "sentinel-2:26379");
    assertThat(sentinel.getDatabase()).isEqualTo(2);
    assertThat(sentinel.getUsername()).isEqualTo("data-user");
    assertThat(sentinel.getPassword()).isEqualTo(RedisPassword.of("data-password"));
    assertThat(sentinel.getSentinelUsername()).isEqualTo("sentinel-user");
    assertThat(sentinel.getSentinelPassword()).isEqualTo(RedisPassword.of("sentinel-password"));
    assertThat(factory.getShareNativeConnection()).isTrue();
    assertThat(factory.getValidateConnection()).isFalse();
  }

  @Test
  @DisplayName("Sentinel master 또는 nodes가 없으면 설정 오류를 알린다")
  void redisConnectionFactory_rejectsIncompleteSentinelDetails() {
    RedisConnectionDetails connectionDetails =
        new RedisConnectionDetails() {
          @Override
          public Sentinel getSentinel() {
            return new Sentinel() {
              @Override
              public int getDatabase() {
                return 0;
              }

              @Override
              public String getMaster() {
                return "";
              }

              @Override
              public List<Node> getNodes() {
                return List.of();
              }

              @Override
              public String getUsername() {
                return null;
              }

              @Override
              public String getPassword() {
                return null;
              }
            };
          }
        };
    RedisConfig redisConfig = new RedisConfig(connectionDetails);
    ReflectionTestUtils.setField(redisConfig, "redisMode", "sentinel");
    ReflectionTestUtils.setField(redisConfig, "redisTimeout", Duration.ofSeconds(15));

    assertThatThrownBy(redisConfig::redisConnectionFactory)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("master와 nodes");
  }

  private RedisConnectionDetails sentinelConnectionDetails(
      String dataUsername, String dataPassword, String sentinelUsername, String sentinelPassword) {
    return new RedisConnectionDetails() {
      @Override
      public String getUsername() {
        return dataUsername;
      }

      @Override
      public String getPassword() {
        return dataPassword;
      }

      @Override
      public Sentinel getSentinel() {
        return new Sentinel() {
          @Override
          public int getDatabase() {
            return 2;
          }

          @Override
          public String getMaster() {
            return "bottlenote-master";
          }

          @Override
          public List<Node> getNodes() {
            return List.of(
                new Node("sentinel-0", 26379),
                new Node("sentinel-1", 26379),
                new Node("sentinel-2", 26379));
          }

          @Override
          public String getUsername() {
            return sentinelUsername;
          }

          @Override
          public String getPassword() {
            return sentinelPassword;
          }
        };
      }
    };
  }
}
