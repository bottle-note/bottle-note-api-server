package app.bottlenote.statistics.facade;

import app.bottlenote.global.timeseries.TimeSeries;
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest;

public interface VisitorStatisticsFacade {

  TimeSeries findActiveVisitors(VisitorStatisticsRequest request);

  TimeSeries findRetention(VisitorStatisticsRequest request);
}
