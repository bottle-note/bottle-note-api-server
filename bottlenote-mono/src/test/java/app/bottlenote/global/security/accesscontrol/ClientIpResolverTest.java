package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
@DisplayName("ClientIpResolver 단위 테스트")
class ClientIpResolverTest {

  @Test
  @DisplayName("X-Forwarded-For 첫 유효 IP를 사용한다")
  void resolve_whenXffPresent_usesFirstValidIp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "unknown, 203.0.113.10, 198.51.100.1");
    request.setRemoteAddr("192.0.2.1");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
  }

  @Test
  @DisplayName("XFF가 없으면 remoteAddr를 사용한다")
  void resolve_whenXffMissing_usesRemoteAddr() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("198.51.100.7");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
  }

  @Test
  @DisplayName("normalize는 IPv6를 정규화한다")
  void normalize_whenIpv6_canonicalizes() {
    assertThat(ClientIpResolver.normalize("2001:0db8:0:0:0:0:0:1")).isEqualTo("2001:db8::1");
  }
}
