package app.bottlenote.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.global.timeseries.TimeSeriesException;
import app.bottlenote.global.timeseries.TimeSeriesExceptionCode;
import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import app.bottlenote.statistics.domain.ActiveVisitorBucket;
import app.bottlenote.statistics.domain.ReturningVisitorBucket;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;
import app.bottlenote.statistics.fixture.InMemoryVisitorStatisticsRepository;
import app.bottlenote.statistics.service.VisitorStatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("방문자 통계 서비스")
class VisitorStatisticsServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private InMemoryVisitorStatisticsRepository repository;
  private VisitorStatisticsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryVisitorStatisticsRepository();
    service = new VisitorStatisticsService(repository);
  }

  @Test
  @DisplayName("값이 없는 버킷이 있을 때 0으로 채워 활동 시계열을 조립한다")
  void 빈_버킷을_0으로_채워_활동_시계열을_조립할_수_있다() {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 3);
    repository.seedActive(new ActiveVisitorBucket(LocalDateTime.of(2026, 9, 2, 0, 0), 12L, 4L));

    TimeSeries series =
        service.findActiveVisitors(
            new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.DAY));

    assertThat(series.points()).hasSize(3);
    assertThat(series.points().get(0).values().get("visitors")).isEqualTo(0L);
    assertThat(series.points().get(0).values().get("members")).isEqualTo(0L);
    assertThat(series.points().get(1).values().get("visitors")).isEqualTo(12L);
    assertThat(series.points().get(1).values().get("members")).isEqualTo(4L);
    assertThat(series.points().get(2).values().get("visitors")).isEqualTo(0L);
    assertThat(series.series())
        .extracting(descriptor -> descriptor.key())
        .containsExactly("visitors", "members");
  }

  @Test
  @DisplayName("재방문율을 계산할 때 소수 1자리로 반올림하고 방문자가 0이면 0이다")
  void 재방문율을_소수_1자리로_계산할_수_있다() {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 2);
    repository.seedReturning(
        new ReturningVisitorBucket(LocalDateTime.of(2026, 9, 1, 0, 0), 3L, 2L),
        new ReturningVisitorBucket(LocalDateTime.of(2026, 9, 2, 0, 0), 0L, 0L));

    TimeSeries series =
        service.findRetention(new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.DAY));

    assertThat(series.points().get(0).values().get("retentionRate")).isEqualTo(66.7);
    assertThat(series.points().get(1).values().get("retentionRate")).isEqualTo(0.0);
  }

  @Test
  @DisplayName("재방문율을 조회할 때 첫 버킷의 직전 버킷부터 읽는다")
  void 재방문율의_첫_버킷을_위해_직전_버킷부터_조회할_수_있다() {
    LocalDate from = LocalDate.of(2026, 9, 2);
    LocalDate to = LocalDate.of(2026, 9, 3);

    service.findRetention(new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.DAY));

    assertThat(repository.lastFrom()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
    assertThat(repository.lastToExclusive()).isEqualTo(LocalDateTime.of(2026, 9, 4, 0, 0));
    assertThat(repository.lastGranularity()).isEqualTo(TimeSeriesGranularity.DAY);
  }

  @Test
  @DisplayName("재방문율 응답 포인트는 요청 구간만 포함하고 직전 버킷은 뺀다")
  void 응답_포인트는_요청_구간만_포함할_수_있다() {
    LocalDate from = LocalDate.of(2026, 9, 2);
    LocalDate to = LocalDate.of(2026, 9, 3);
    repository.seedReturning(
        new ReturningVisitorBucket(LocalDateTime.of(2026, 9, 1, 0, 0), 10L, 3L),
        new ReturningVisitorBucket(LocalDateTime.of(2026, 9, 2, 0, 0), 4L, 1L),
        new ReturningVisitorBucket(LocalDateTime.of(2026, 9, 3, 0, 0), 5L, 2L));

    TimeSeries series =
        service.findRetention(new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.DAY));

    assertThat(series.points()).hasSize(2);
    assertThat(series.points())
        .extracting(point -> point.bucketAt())
        .containsExactly("2026-09-02T00:00:00", "2026-09-03T00:00:00");
    assertThat(series.points().get(0).values().get("returningVisitors")).isEqualTo(1L);
  }

  @Test
  @DisplayName("집계 단위에 따라 방문자 시리즈 label이 DAU, WAU, MAU로 바뀐다")
  void granularity별_시리즈_label을_쓸_수_있다() {
    LocalDate from = LocalDate.of(2026, 8, 1);
    LocalDate to = LocalDate.of(2026, 8, 10);

    TimeSeries day =
        service.findActiveVisitors(
            new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.DAY));
    TimeSeries week =
        service.findActiveVisitors(
            new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.WEEK));
    TimeSeries month =
        service.findActiveVisitors(
            new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.MONTH));
    TimeSeries retention =
        service.findRetention(new VisitorStatisticsRequest(from, to, TimeSeriesGranularity.WEEK));

    assertThat(day.series())
        .extracting(descriptor -> descriptor.label())
        .containsExactly("방문자 DAU", "회원 DAU");
    assertThat(week.series())
        .extracting(descriptor -> descriptor.label())
        .containsExactly("방문자 WAU", "회원 WAU");
    assertThat(month.series())
        .extracting(descriptor -> descriptor.label())
        .containsExactly("방문자 MAU", "회원 MAU");
    assertThat(retention.series())
        .extracting(descriptor -> descriptor.label())
        .containsExactly("방문자 WAU", "재방문자", "재방문율");
  }

  @Test
  @DisplayName("조회 구간이 90일을 넘으면 RANGE_TOO_LONG이다")
  void 구간이_90일을_넘으면_예외를_던질_수_있다() {
    LocalDate today = LocalDate.now(ZONE);
    VisitorStatisticsRequest request =
        new VisitorStatisticsRequest(today.minusDays(90), today, TimeSeriesGranularity.DAY);

    assertThatThrownBy(() -> service.findActiveVisitors(request))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", TimeSeriesExceptionCode.RANGE_TOO_LONG);
  }

  @Test
  @DisplayName("HOUR 단위를 요청하면 UNSUPPORTED_GRANULARITY다")
  void HOUR_요청이면_예외를_던질_수_있다() {
    VisitorStatisticsRequest request =
        new VisitorStatisticsRequest(
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), TimeSeriesGranularity.HOUR);

    assertThatThrownBy(() -> service.findActiveVisitors(request))
        .isInstanceOf(TimeSeriesException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", TimeSeriesExceptionCode.UNSUPPORTED_GRANULARITY);
  }
}
