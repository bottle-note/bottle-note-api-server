package app.bottlenote.statistics.dto.request;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.time.LocalDate;

public record VisitorStatisticsRequest(
    LocalDate from, LocalDate to, TimeSeriesGranularity granularity) {

  public VisitorStatisticsRequest {
    granularity = granularity == null ? TimeSeriesGranularity.DAY : granularity;
  }
}
