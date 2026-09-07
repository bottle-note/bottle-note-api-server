package app.bottlenote.alcohols.dto.request;

import app.bottlenote.global.timeseries.TimeSeriesGranularity;
import java.time.LocalDate;

public record AlcoholPopularityTimeSeriesRequest(
    LocalDate from, LocalDate to, TimeSeriesGranularity granularity) {

  public AlcoholPopularityTimeSeriesRequest {
    granularity = granularity != null ? granularity : TimeSeriesGranularity.WEEK;
  }
}
