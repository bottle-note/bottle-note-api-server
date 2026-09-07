package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservation;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 참여도 관측 포트의 상태 기반 테스트 더블 */
public class InMemoryAlcoholEngagementObservationRepository
    implements AlcoholEngagementObservationRepository {

  private final List<AlcoholEngagementObservation> rows = new ArrayList<>();

  @Override
  public Optional<AlcoholEngagementObservation> findByAlcoholIdAndBucketGranularityAndBucketAt(
      Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().equals(bucketAt)
                    && row.getAlcoholId().equals(alcoholId))
        .findFirst();
  }

  @Override
  public List<AlcoholEngagementObservation> findByBucketGranularityAndBucketAt(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().equals(bucketAt))
        .toList();
  }

  @Override
  public List<AlcoholEngagementObservation>
      findByAlcoholIdAndBucketGranularityAndBucketAtBetweenOrderByBucketAtAsc(
          Long alcoholId,
          BucketGranularity bucketGranularity,
          LocalDateTime from,
          LocalDateTime to) {
    return rows.stream()
        .filter(
            row ->
                row.getAlcoholId().equals(alcoholId)
                    && row.getBucketGranularity() == bucketGranularity
                    && !row.getBucketAt().isBefore(from)
                    && !row.getBucketAt().isAfter(to))
        .sorted(Comparator.comparing(AlcoholEngagementObservation::getBucketAt))
        .toList();
  }

  @Override
  public Optional<AlcoholEngagementObservation>
      findTopByAlcoholIdAndBucketGranularityAndBucketAtLessThanOrderByBucketAtDesc(
          Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getAlcoholId().equals(alcoholId)
                    && row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().isBefore(bucketAt))
        .max(Comparator.comparing(AlcoholEngagementObservation::getBucketAt));
  }

  @Override
  public AlcoholEngagementObservation save(AlcoholEngagementObservation observation) {
    rows.removeIf(
        saved ->
            saved.getBucketGranularity() == observation.getBucketGranularity()
                && saved.getBucketAt().equals(observation.getBucketAt())
                && saved.getAlcoholId().equals(observation.getAlcoholId()));
    rows.add(observation);
    return observation;
  }
}
