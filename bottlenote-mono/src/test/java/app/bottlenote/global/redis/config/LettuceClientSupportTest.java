package app.bottlenote.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Tag("unit")
@DisplayName("LettuceClientSupport 소켓 복원력 정책 테스트")
class LettuceClientSupportTest {

  @Test
  @DisplayName("KeepAlive는 idle 60초, interval 10초, count 3이고 TcpUserTimeout은 30초로 켠다")
  void socketOptions_enablesKeepAliveAndTcpUserTimeout() {
    SocketOptions socketOptions = LettuceClientSupport.socketOptions(Duration.ofSeconds(15));

    assertThat(socketOptions.getKeepAlive().isEnabled()).isTrue();
    assertThat(socketOptions.getKeepAlive().getIdle()).isEqualTo(Duration.ofSeconds(60));
    assertThat(socketOptions.getKeepAlive().getInterval()).isEqualTo(Duration.ofSeconds(10));
    assertThat(socketOptions.getKeepAlive().getCount()).isEqualTo(3);
    assertThat(socketOptions.getConnectTimeout()).isEqualTo(Duration.ofSeconds(15));
  }

  @Test
  @DisplayName("TcpUserTimeout은 in-flight 반열림을 끊기 위해 30초로 켜 둔다")
  void socketOptions_boundsUnacknowledgedDataWithTcpUserTimeout() {
    SocketOptions socketOptions = LettuceClientSupport.socketOptions(Duration.ofSeconds(15));

    assertThat(socketOptions.getTcpUserTimeout().isEnabled()).isTrue();
    assertThat(socketOptions.getTcpUserTimeout().getTcpUserTimeout())
        .isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  @DisplayName("epoll 가용 여부 확인은 클래스가 없어도 예외 없이 판정한다")
  void isEpollAvailable_neverThrows() {
    assertThatCode(LettuceClientSupport::isEpollAvailable).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("끊긴 연결에서는 명령을 거절하고 요청 큐를 유한하게 둔다")
  void clientOptions_rejectsCommandsWhenDisconnectedAndBoundsQueue() {
    ClientOptions clientOptions = LettuceClientSupport.clientOptions(Duration.ofSeconds(2));

    assertThat(clientOptions.getDisconnectedBehavior())
        .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    assertThat(clientOptions.getRequestQueueSize()).isEqualTo(2048);
    assertThat(clientOptions.isAutoReconnect()).isTrue();
    assertThat(clientOptions.isPingBeforeActivateConnection()).isTrue();
  }

  @Test
  @DisplayName("전용 factory는 command timeout만 바꾸고 공용 소켓 정책과 연결 공유를 유지한다")
  void dedicatedFactory_reusesSocketPolicyWithCallerTimeout() {
    LettuceClientConfiguration sourceConfig =
        LettuceClientSupport.clientConfiguration(Duration.ofSeconds(15));
    LettuceConnectionFactory source =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("127.0.0.1", 6379), sourceConfig);
    LettuceClientSupport.start(source);

    try {
      LettuceConnectionFactory dedicated =
          LettuceClientSupport.dedicatedFactory(source, Duration.ofMillis(200));
      LettuceClientSupport.start(dedicated);
      try {
        ClientOptions options = dedicated.getClientConfiguration().getClientOptions().orElseThrow();
        assertThat(dedicated.getClientConfiguration().getCommandTimeout())
            .isEqualTo(Duration.ofMillis(200));
        assertThat(options.getSocketOptions().getKeepAlive().isEnabled()).isTrue();
        assertThat(options.getDisconnectedBehavior())
            .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
        assertThat(dedicated.getShareNativeConnection()).isTrue();
        assertThat(dedicated.getValidateConnection()).isFalse();
        assertThat(dedicated).isNotSameAs(source);
      } finally {
        dedicated.destroy();
      }
    } finally {
      source.destroy();
    }
  }

  @Test
  @DisplayName("Sentinel 전용 factory는 master, nodes와 두 종류의 인증을 그대로 유지한다")
  void dedicatedFactory_preservesSentinelDiscoveryAndCredentials() {
    RedisSentinelConfiguration sentinel = new RedisSentinelConfiguration();
    sentinel.master("bottlenote-master");
    sentinel.sentinel("sentinel-0", 26379);
    sentinel.sentinel("sentinel-1", 26379);
    sentinel.setDatabase(2);
    sentinel.setUsername("data-user");
    sentinel.setPassword(RedisPassword.of("data-password"));
    sentinel.setSentinelUsername("sentinel-user");
    sentinel.setSentinelPassword(RedisPassword.of("sentinel-password"));
    LettuceConnectionFactory source =
        new LettuceConnectionFactory(
            sentinel, LettuceClientSupport.clientConfiguration(Duration.ofSeconds(15)));
    LettuceClientSupport.start(source);

    try {
      LettuceConnectionFactory dedicated =
          LettuceClientSupport.dedicatedFactory(source, Duration.ofMillis(200));
      LettuceClientSupport.start(dedicated);
      try {
        RedisSentinelConfiguration dedicatedSentinel = dedicated.getSentinelConfiguration();
        assertThat(dedicatedSentinel).isNotNull();
        assertThat(dedicatedSentinel.getMaster().getName()).isEqualTo("bottlenote-master");
        assertThat(dedicatedSentinel.getSentinels()).hasSize(2);
        assertThat(dedicatedSentinel.getDatabase()).isEqualTo(2);
        assertThat(dedicatedSentinel.getUsername()).isEqualTo("data-user");
        assertThat(dedicatedSentinel.getPassword()).isEqualTo(RedisPassword.of("data-password"));
        assertThat(dedicatedSentinel.getSentinelUsername()).isEqualTo("sentinel-user");
        assertThat(dedicatedSentinel.getSentinelPassword())
            .isEqualTo(RedisPassword.of("sentinel-password"));
        assertThat(dedicated.getClientConfiguration().getCommandTimeout())
            .isEqualTo(Duration.ofMillis(200));
        assertThat(dedicated.getShareNativeConnection()).isTrue();
        assertThat(dedicated.getValidateConnection()).isFalse();
      } finally {
        dedicated.destroy();
      }
    } finally {
      source.destroy();
    }
  }
}
