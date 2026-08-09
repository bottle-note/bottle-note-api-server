package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AccessControlProperties 단위 테스트")
class AccessControlPropertiesTest {

  @Test
  @DisplayName("redisCommandTimeout 기본값은 200ms 이다")
  void redisCommandTimeout_defaultIs200ms() {
    AccessControlProperties properties = new AccessControlProperties();

    assertThat(properties.getRedisCommandTimeout()).isEqualTo(Duration.ofMillis(200));
  }

  @Test
  @DisplayName("rateLimitConnectionCount 기본값은 4 이다")
  void rateLimitConnectionCount_defaultIsFour() {
    assertThat(new AccessControlProperties().getRateLimitConnectionCount()).isEqualTo(4);
  }

  @Test
  @DisplayName("failOpen 기본값은 true 이다")
  void failOpen_defaultIsTrue() {
    assertThat(new AccessControlProperties().isFailOpen()).isTrue();
  }

  @Test
  @DisplayName("snapshot 기본값은 30초 갱신, 3분 stale, 10000건 상한이다")
  void snapshot_defaultsAreConfigured() {
    AccessControlProperties.Snapshot snapshot = new AccessControlProperties().getSnapshot();

    assertThat(snapshot.getRefreshIntervalMs()).isEqualTo(30_000L);
    assertThat(snapshot.getStaleThreshold()).isEqualTo(Duration.ofMinutes(3));
    assertThat(snapshot.getMaxEntries()).isEqualTo(10_000);
  }

  @Test
  @DisplayName("burst admission 기본값은 32 동시 요청과 1초 cooldown 이다")
  void burstAdmission_defaultsAreConfigured() {
    AccessControlProperties.BurstAdmission admission =
        new AccessControlProperties().getBurstAdmission();

    assertThat(admission.getMaxConcurrent()).isEqualTo(32);
    assertThat(admission.getCooldown()).isEqualTo(Duration.ofSeconds(1));
  }
}
