package app.bottlenote.global.timeseries;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("시계열 조립")
class TimeSeriesAssemblerTest {

  private static final TimeSeriesRange DAY_RANGE =
      new TimeSeriesRange(
          LocalDateTime.of(2026, 9, 1, 0, 0),
          LocalDateTime.of(2026, 9, 3, 0, 0),
          TimeSeriesGranularity.DAY);
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 3, 12, 0);

  @Test
  @DisplayName("ZERO 채움은 없는 값을 Long 0으로 채운다")
  void ZERO_채움을_적용할_수_있다() {
    TimeSeriesDescriptor visitors =
        new TimeSeriesDescriptor("visitors", "방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO);

    TimeSeries series =
        TimeSeriesAssembler.assemble(
            DAY_RANGE,
            List.of(visitors),
            Map.of(LocalDateTime.of(2026, 9, 2, 0, 0), Map.of("visitors", 12)),
            NOW);

    assertThat(series.points())
        .extracting(point -> point.values().get("visitors"))
        .containsExactly(0L, 12, 0L);
  }

  @Test
  @DisplayName("NULL 채움은 없는 값을 null로 둔다")
  void NULL_채움을_적용할_수_있다() {
    TimeSeriesDescriptor score =
        new TimeSeriesDescriptor("score", "점수", TimeSeriesUnit.SCORE, TimeSeriesFill.NULL);

    TimeSeries series =
        TimeSeriesAssembler.assemble(
            DAY_RANGE,
            List.of(score),
            Map.of(LocalDateTime.of(2026, 9, 2, 0, 0), Map.of("score", 0.8)),
            NOW);

    assertThat(series.points().get(0).values().get("score")).isNull();
    assertThat(series.points().get(1).values().get("score")).isEqualTo(0.8);
    assertThat(series.points().get(2).values().get("score")).isNull();
  }

  @Test
  @DisplayName("PREVIOUS 채움은 구간 안 직전 값을 쓰고 첫 값 전에는 null이다")
  void PREVIOUS_채움을_적용할_수_있다() {
    TimeSeriesDescriptor state =
        new TimeSeriesDescriptor("picks", "픽", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS);

    TimeSeries series =
        TimeSeriesAssembler.assemble(
            DAY_RANGE,
            List.of(state),
            Map.of(LocalDateTime.of(2026, 9, 2, 0, 0), Map.of("picks", 7)),
            NOW);

    assertThat(series.points().get(0).values().get("picks")).isNull();
    assertThat(series.points().get(1).values().get("picks")).isEqualTo(7);
    assertThat(series.points().get(2).values().get("picks")).isEqualTo(7);
  }

  @Test
  @DisplayName("빠진 버킷도 구간의 모든 포인트를 순서대로 채운다")
  void 빠진_버킷을_채울_수_있다() {
    TimeSeriesDescriptor visitors =
        new TimeSeriesDescriptor("visitors", "방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO);

    TimeSeries series = TimeSeriesAssembler.assemble(DAY_RANGE, List.of(visitors), Map.of(), NOW);

    assertThat(series.points())
        .extracting(TimeSeriesPoint::bucketAt)
        .containsExactly("2026-09-01T00:00:00", "2026-09-02T00:00:00", "2026-09-03T00:00:00");
    assertThat(series.from()).isEqualTo("2026-09-01T00:00:00");
    assertThat(series.to()).isEqualTo("2026-09-03T00:00:00");
    assertThat(series.timezone()).isEqualTo("Asia/Seoul");
  }

  @Test
  @DisplayName("버킷 종료가 now보다 뒤이면 partial이고 종료 시각과 같으면 닫힌다")
  void partial을_판정할_수_있다() {
    TimeSeriesDescriptor visitors =
        new TimeSeriesDescriptor("visitors", "방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO);
    LocalDateTime bucketEnd = LocalDateTime.of(2026, 9, 4, 0, 0);

    TimeSeries open =
        TimeSeriesAssembler.assemble(
            DAY_RANGE, List.of(visitors), Map.of(), LocalDateTime.of(2026, 9, 3, 23, 59, 59));
    TimeSeries closed =
        TimeSeriesAssembler.assemble(DAY_RANGE, List.of(visitors), Map.of(), bucketEnd);

    assertThat(open.points())
        .extracting(TimeSeriesPoint::partial)
        .containsExactly(false, false, true);
    assertThat(closed.points())
        .extracting(TimeSeriesPoint::partial)
        .containsExactly(false, false, false);
  }

  @Test
  @DisplayName("명시적 null 값도 fill 규칙을 탄다")
  void 명시적_null도_fill을_적용할_수_있다() {
    TimeSeriesDescriptor visitors =
        new TimeSeriesDescriptor("visitors", "방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO);
    TimeSeriesDescriptor picks =
        new TimeSeriesDescriptor("picks", "픽", TimeSeriesUnit.COUNT, TimeSeriesFill.PREVIOUS);
    Map<LocalDateTime, Map<String, Number>> valuesByBucket = new LinkedHashMap<>();
    Map<String, Number> first = new LinkedHashMap<>();
    first.put("visitors", 5);
    first.put("picks", 7);
    valuesByBucket.put(LocalDateTime.of(2026, 9, 1, 0, 0), first);
    Map<String, Number> second = new LinkedHashMap<>();
    second.put("visitors", null);
    second.put("picks", null);
    valuesByBucket.put(LocalDateTime.of(2026, 9, 2, 0, 0), second);

    TimeSeries series =
        TimeSeriesAssembler.assemble(DAY_RANGE, List.of(visitors, picks), valuesByBucket, NOW);

    assertThat(series.points().get(1).values().get("visitors")).isEqualTo(0L);
    assertThat(series.points().get(1).values().get("picks")).isEqualTo(7);
  }

  @Test
  @DisplayName("values는 시리즈 key 순서를 유지하고 null 값을 허용한다")
  void values의_순서와_null을_유지할_수_있다() {
    TimeSeriesDescriptor visitors =
        new TimeSeriesDescriptor("visitors", "방문자", TimeSeriesUnit.COUNT, TimeSeriesFill.ZERO);
    TimeSeriesDescriptor retention =
        new TimeSeriesDescriptor("retention", "재방문율", TimeSeriesUnit.PERCENT, TimeSeriesFill.NULL);
    Map<String, Number> source = new LinkedHashMap<>();
    source.put("retention", null);
    source.put("visitors", 31);

    TimeSeries series =
        TimeSeriesAssembler.assemble(
            new TimeSeriesRange(
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 0, 0),
                TimeSeriesGranularity.DAY),
            List.of(visitors, retention),
            Map.of(LocalDateTime.of(2026, 9, 1, 0, 0), source),
            NOW);

    TimeSeriesPoint point = series.points().getFirst();
    assertThat(point.values().keySet()).containsExactly("visitors", "retention");
    assertThat(point.values().get("visitors")).isEqualTo(31);
    assertThat(point.values().get("retention")).isNull();
    assertThat(new HashMap<>(point.values())).containsEntry("retention", null);
  }
}
