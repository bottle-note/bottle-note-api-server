package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholRatingObservation;
import app.bottlenote.alcohols.domain.AlcoholRatingObservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 평가도 관측 포트의 상태 기반 테스트 더블 */
public class InMemoryAlcoholRatingObservationRepository
    implements AlcoholRatingObservationRepository {

  private final List<AlcoholRatingObservation> rows = new ArrayList<>();

  @Override
  public Optional<AlcoholRatingObservation> findByAlcoholIdAndBucketGranularityAndBucketAt(
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
  public List<AlcoholRatingObservation> findByBucketGranularityAndBucketAt(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().equals(bucketAt))
        .toList();
  }

  @Override
  public List<AlcoholRatingObservation>
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
        .sorted(Comparator.comparing(AlcoholRatingObservation::getBucketAt))
        .toList();
  }

  @Override
  public Optional<AlcoholRatingObservation>
      findTopByAlcoholIdAndBucketGranularityAndBucketAtLessThanOrderByBucketAtDesc(
          Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getAlcoholId().equals(alcoholId)
                    && row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().isBefore(bucketAt))
        .max(Comparator.comparing(AlcoholRatingObservation::getBucketAt));
  }

  @Override
  public AlcoholRatingObservation save(AlcoholRatingObservation observation) {
    rows.removeIf(
        saved ->
            saved.getBucketGranularity() == observation.getBucketGranularity()
                && saved.getBucketAt().equals(observation.getBucketAt())
                && saved.getAlcoholId().equals(observation.getAlcoholId()));
    rows.add(observation);
    return observation;
  }
}
