package app.bottlenote.alcohols.facade;

import app.bottlenote.alcohols.constant.PopularityAxis;
import app.bottlenote.alcohols.dto.request.AlcoholPopularityTimeSeriesRequest;
import app.bottlenote.global.timeseries.TimeSeries;
import java.util.List;

/** 타 도메인에 공개하는 인기도 조회 계약 */
public interface AlcoholPopularityFacade {

  List<Long> findWeeklyTopAlcoholIds(int limit);

  TimeSeries findPopularity(Long alcoholId, AlcoholPopularityTimeSeriesRequest request);

  TimeSeries findObservations(
      Long alcoholId, PopularityAxis axis, AlcoholPopularityTimeSeriesRequest request);
}
