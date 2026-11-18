package app.bottlenote.global.timeseries;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record TimeSeries(
    TimeSeriesGranularity granularity,
    String timezone,
    String from,
    String to,
    List<TimeSeriesDescriptor> series,
    List<TimeSeriesPoint> points) {

  public static final String TIMEZONE = "Asia/Seoul";
  // ISO_LOCAL_DATE_TIME은 초가 0이면 초를 생략하므로 고정 패턴을 쓴다.
  public static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public TimeSeries {
    series = series == null ? List.of() : List.copyOf(series);
    points = points == null ? List.of() : List.copyOf(points);
  }

  public static String format(LocalDateTime dateTime) {
    return DATE_TIME_FORMAT.format(dateTime);
  }
}
