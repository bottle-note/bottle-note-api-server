package app.bottlenote.history.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.history.exception.UserHistoryException;
import app.bottlenote.history.exception.UserHistoryExceptionCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("UserHistorySearchRequest 기간 처리 단위 테스트")
class UserHistorySearchRequestTest {

  @Test
  @DisplayName("기간을 보내지 않으면 startDate와 endDate는 null로 유지한다")
  void 기간을_보내지_않으면_원본_날짜는_null이다() {
    UserHistorySearchRequest request = requestOf(null, null);

    assertThat(request.startDate()).isNull();
    assertThat(request.endDate()).isNull();
  }

  @Test
  @DisplayName("기간을 보내지 않으면 조회용 기간은 최근 2년으로 채운다")
  void 기간을_보내지_않으면_조회용_기간은_기본창이다() {
    LocalDateTime before = LocalDateTime.now().minusYears(2).minusSeconds(1);
    UserHistorySearchRequest request = requestOf(null, null);
    LocalDateTime after = LocalDateTime.now().plusDays(1).plusSeconds(1);

    assertThat(request.resolvedStartDate()).isAfter(before);
    assertThat(request.resolvedEndDate()).isBefore(after);
  }

  @Test
  @DisplayName("요청에 들어온 기간은 그대로 두고 종료일만 조회 시 하루를 더한다")
  void 요청_기간은_유지하고_조회_종료일만_하루를_더한다() {
    LocalDateTime start = LocalDateTime.parse("2026-01-01T00:00:00");
    LocalDateTime end = LocalDateTime.parse("2026-01-31T00:00:00");
    UserHistorySearchRequest request = requestOf(start, end);

    assertThat(request.startDate()).isEqualTo(start);
    assertThat(request.endDate()).isEqualTo(end);
    assertThat(request.resolvedStartDate()).isEqualTo(start);
    assertThat(request.resolvedEndDate()).isEqualTo(end.plusDays(1));
  }

  @Test
  @DisplayName("종료일이 시작일보다 앞서면 INVALID_HISTORY_DATE를 던진다")
  void 종료일이_시작일보다_앞서면_예외다() {
    LocalDateTime start = LocalDateTime.parse("2026-02-01T00:00:00");
    LocalDateTime end = LocalDateTime.parse("2026-01-01T00:00:00");

    assertThatThrownBy(() -> requestOf(start, end))
        .isInstanceOf(UserHistoryException.class)
        .extracting("exceptionCode")
        .isEqualTo(UserHistoryExceptionCode.INVALID_HISTORY_DATE);
  }

  @Test
  @DisplayName("기간이 2년을 넘으면 INVALID_HISTORY_DATE_RANGE를 던진다")
  void 기간이_2년을_넘으면_예외다() {
    LocalDateTime start = LocalDateTime.parse("2020-01-01T00:00:00");
    LocalDateTime end = LocalDateTime.parse("2023-01-01T00:00:00");

    assertThatThrownBy(() -> requestOf(start, end))
        .isInstanceOf(UserHistoryException.class)
        .extracting("exceptionCode")
        .isEqualTo(UserHistoryExceptionCode.INVALID_HISTORY_DATE_RANGE);
  }

  private static UserHistorySearchRequest requestOf(LocalDateTime start, LocalDateTime end) {
    return new UserHistorySearchRequest(null, null, null, null, start, end, null, null, null);
  }
}
