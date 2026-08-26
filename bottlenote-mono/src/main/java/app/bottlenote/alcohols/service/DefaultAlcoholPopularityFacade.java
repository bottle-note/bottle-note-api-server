package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshotRepository;
import app.bottlenote.alcohols.facade.AlcoholPopularityFacade;
import app.bottlenote.common.annotation.FacadeService;
import java.util.List;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultAlcoholPopularityFacade implements AlcoholPopularityFacade {

  private final AlcoholPopularitySnapshotRepository snapshotRepository;

  @Override
  public List<Long> findWeeklyTopAlcoholIds(int limit) {
    return snapshotRepository.findLatestTopAlcoholIds(BucketGranularity.WEEK, limit);
  }
}
