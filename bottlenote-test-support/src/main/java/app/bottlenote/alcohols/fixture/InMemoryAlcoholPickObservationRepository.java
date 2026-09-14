package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholPickObservation;
import app.bottlenote.alcohols.domain.AlcoholPickObservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 선호도 관측 포트의 상태 기반 테스트 더블 */
public class InMemoryAlcoholPickObservationRepository implements AlcoholPickObservationRepository {

  private final List<AlcoholPickObservation> rows = new ArrayList<>();

  @Override
  public Optional<AlcoholPickObservation> findByAlcoholIdAndBucketGranularityAndBucketAt(
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
  public List<AlcoholPickObservation> findByBucketGranularityAndBucketAt(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().equals(bucketAt))
        .toList();
  }

  @Override
  public List<AlcoholPickObservation>
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
        .sorted(Comparator.comparing(AlcoholPickObservation::getBucketAt))
        .toList();
  }

  @Override
  public Optional<AlcoholPickObservation>
      findTopByAlcoholIdAndBucketGranularityAndBucketAtLessThanOrderByBucketAtDesc(
          Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return rows.stream()
        .filter(
            row ->
                row.getAlcoholId().equals(alcoholId)
                    && row.getBucketGranularity() == bucketGranularity
                    && row.getBucketAt().isBefore(bucketAt))
        .max(Comparator.comparing(AlcoholPickObservation::getBucketAt));
  }

  @Override
  public AlcoholPickObservation save(AlcoholPickObservation observation) {
    rows.removeIf(
        saved ->
            saved.getBucketGranularity() == observation.getBucketGranularity()
                && saved.getBucketAt().equals(observation.getBucketAt())
                && saved.getAlcoholId().equals(observation.getAlcoholId()));
    rows.add(observation);
    return observation;
  }
}
