package app.bottlenote.history.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.history.dto.request.UserHistorySearchRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("히스토리 HMAC context 단위 테스트")
class HistoryContextTest {

  @Test
  @DisplayName("기간이 다른 요청은 HMAC context가 다르다")
  void 기간이_다른_요청은_context가_다르다() {
    UserHistorySearchRequest oneMonth =
        requestOf(
            LocalDateTime.parse("2026-07-01T00:00:00"), LocalDateTime.parse("2026-07-31T00:00:00"));
    UserHistorySearchRequest twoYears =
        requestOf(
            LocalDateTime.parse("2024-08-01T00:00:00"), LocalDateTime.parse("2026-08-01T00:00:00"));

    String contextA = CustomUserHistoryRepositoryImpl.historyContext(1L, oneMonth);
    String contextB = CustomUserHistoryRepositoryImpl.historyContext(1L, twoYears);

    assertThat(contextA).isNotEqualTo(contextB);
    assertThat(contextA).contains("2026-07-01T00:00", "2026-07-31T00:00");
  }

  @Test
  @DisplayName("기간을 보내지 않은 요청의 context는 시각이 지나도 같다")
  void 기간을_보내지_않으면_context는_안정적이다() {
    UserHistorySearchRequest first = requestOf(null, null);
    UserHistorySearchRequest second = requestOf(null, null);

    assertThat(CustomUserHistoryRepositoryImpl.historyContext(1L, first))
        .isEqualTo(CustomUserHistoryRepositoryImpl.historyContext(1L, second))
        .endsWith("null:null");
  }

  private static UserHistorySearchRequest requestOf(LocalDateTime start, LocalDateTime end) {
    return new UserHistorySearchRequest(null, null, null, null, start, end, null, null, null);
  }
}
