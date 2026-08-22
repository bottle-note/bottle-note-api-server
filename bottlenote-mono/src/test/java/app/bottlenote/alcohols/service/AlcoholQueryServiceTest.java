package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AlcoholQueryService 단위 테스트")
class AlcoholQueryServiceTest {

  @Test
  @DisplayName("RANDOM 첫 페이지 seed는 고정된 서버 시각의 epoch second를 사용한다")
  void resolveFirstPageRandomSeed_usesServerEpochSecond() {
    // given
    Instant serverTime = Instant.ofEpochSecond(1_724_123_456L);

    // when
    long seed = AlcoholQueryService.resolveFirstPageRandomSeed(serverTime);

    // then
    assertThat(seed).isEqualTo(1_724_123_456L);
  }
}
