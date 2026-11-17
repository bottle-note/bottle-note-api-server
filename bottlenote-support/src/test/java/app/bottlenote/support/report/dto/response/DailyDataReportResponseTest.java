package app.bottlenote.support.report.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DailyDataReportResponse 단위 테스트")
class DailyDataReportResponseTest {

  @Test
  @DisplayName("별점 이벤트만 발생해도 신규 데이터가 있다고 판단한다")
  void 별점_이벤트만_발생해도_신규_데이터가_있다() {
    DailyDataReportResponse response = reportWithRatingEvents(1L);

    assertThat(response.hasNewData()).isTrue();
  }

  @Test
  @DisplayName("별점 이벤트가 없으면 메시지에 0건으로 표시한다")
  void 별점_이벤트가_없으면_0건으로_표시한다() {
    DailyDataReportResponse response = reportWithRatingEvents(0L);

    assertThat(response.toDiscordMessage()).contains("별점 이벤트**: 0건");
  }

  private DailyDataReportResponse reportWithRatingEvents(Long ratingEventsCount) {
    return new DailyDataReportResponse(
        LocalDate.of(2026, 8, 8), 0L, 0L, 0L, 0L, ratingEventsCount, 0L, 0L);
  }
}
