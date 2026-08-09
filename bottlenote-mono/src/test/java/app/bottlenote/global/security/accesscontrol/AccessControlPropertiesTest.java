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
  @DisplayName("failOpen 기본값은 true 이다")
  void failOpen_defaultIsTrue() {
    assertThat(new AccessControlProperties().isFailOpen()).isTrue();
  }
}
