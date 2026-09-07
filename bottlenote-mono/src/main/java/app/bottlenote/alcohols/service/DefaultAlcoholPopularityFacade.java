package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.constant.PopularityAxis;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshotRepository;
import app.bottlenote.alcohols.dto.request.AlcoholPopularityTimeSeriesRequest;
import app.bottlenote.alcohols.facade.AlcoholPopularityFacade;
import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.global.timeseries.TimeSeries;
import java.util.List;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultAlcoholPopularityFacade implements AlcoholPopularityFacade {

  private final AlcoholPopularitySnapshotRepository snapshotRepository;
  private final AlcoholPopularityTimeSeriesService timeSeriesService;

  @Override
  public List<Long> findWeeklyTopAlcoholIds(int limit) {
    return snapshotRepository.findLatestTopAlcoholIds(BucketGranularity.WEEK, limit);
  }

  @Override
  public TimeSeries findPopularity(Long alcoholId, AlcoholPopularityTimeSeriesRequest request) {
    return timeSeriesService.findPopularity(alcoholId, request);
  }

  @Override
  public TimeSeries findObservations(
      Long alcoholId, PopularityAxis axis, AlcoholPopularityTimeSeriesRequest request) {
    return timeSeriesService.findObservations(alcoholId, axis, request);
  }
}
