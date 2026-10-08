package app.bottlenote.alcohols.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("위스키 상세 호출자 헤더 판정")
class AlcoholQueryControllerTest {

  @Test
  @DisplayName("호출자 헤더가 ssr이면 대소문자와 앞뒤 공백을 무시해 SSR로 판정한다")
  void SSR_호출자를_판정할_수_있다() {
    assertThat(AlcoholQueryController.isSsrCaller("ssr")).isTrue();
    assertThat(AlcoholQueryController.isSsrCaller("SSR")).isTrue();
    assertThat(AlcoholQueryController.isSsrCaller(" ssr ")).isTrue();
    assertThat(AlcoholQueryController.isSsrCaller(null)).isFalse();
    assertThat(AlcoholQueryController.isSsrCaller("web")).isFalse();
  }
}
