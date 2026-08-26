package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshot;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshotRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Snapshot 조회 포트의 상태 기반 테스트 더블 */
public class InMemoryAlcoholPopularitySnapshotRepository
    implements AlcoholPopularitySnapshotRepository {

  private final List<AlcoholPopularitySnapshot> snapshots = new ArrayList<>();

  @Override
  public Optional<AlcoholPopularitySnapshot> findByAlcoholIdAndBucketGranularityAndBucketAt(
      Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return snapshots.stream()
        .filter(
            snapshot ->
                snapshot.getBucketGranularity() == bucketGranularity
                    && snapshot.getBucketAt().equals(bucketAt)
                    && snapshot.getAlcoholId().equals(alcoholId))
        .findFirst();
  }

  @Override
  public List<AlcoholPopularitySnapshot> findByBucketGranularityAndBucketAt(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return snapshots.stream()
        .filter(
            snapshot ->
                snapshot.getBucketGranularity() == bucketGranularity
                    && snapshot.getBucketAt().equals(bucketAt))
        .toList();
  }

  @Override
  public List<AlcoholPopularitySnapshot>
      findByAlcoholIdAndBucketGranularityAndBucketAtBetweenOrderByBucketAtAsc(
          Long alcoholId,
          BucketGranularity bucketGranularity,
          LocalDateTime from,
          LocalDateTime to) {
    return snapshots.stream()
        .filter(
            snapshot ->
                snapshot.getAlcoholId().equals(alcoholId)
                    && snapshot.getBucketGranularity() == bucketGranularity
                    && !snapshot.getBucketAt().isBefore(from)
                    && !snapshot.getBucketAt().isAfter(to))
        .sorted(Comparator.comparing(AlcoholPopularitySnapshot::getBucketAt))
        .toList();
  }

  @Override
  public AlcoholPopularitySnapshot save(AlcoholPopularitySnapshot snapshot) {
    snapshots.removeIf(
        saved ->
            saved.getBucketGranularity() == snapshot.getBucketGranularity()
                && saved.getBucketAt().equals(snapshot.getBucketAt())
                && saved.getAlcoholId().equals(snapshot.getAlcoholId()));
    snapshots.add(snapshot);
    return snapshot;
  }

  @Override
  public Optional<LocalDateTime> findLatestBucketAt(BucketGranularity bucketGranularity) {
    return snapshots.stream()
        .filter(snapshot -> snapshot.getBucketGranularity() == bucketGranularity)
        .map(AlcoholPopularitySnapshot::getBucketAt)
        .max(LocalDateTime::compareTo);
  }

  @Override
  public List<AlcoholPopularitySnapshot> findTopByBucket(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return snapshots.stream()
        .filter(
            snapshot ->
                snapshot.getBucketGranularity() == bucketGranularity
                    && snapshot.getBucketAt().equals(bucketAt))
        .sorted(
            Comparator.comparing(AlcoholPopularitySnapshot::getPopularityScore)
                .reversed()
                .thenComparing(AlcoholPopularitySnapshot::getAlcoholId))
        .limit(limit)
        .toList();
  }

  @Override
  public List<Long> findLatestTopAlcoholIds(BucketGranularity bucketGranularity, int limit) {
    return findLatestBucketAt(bucketGranularity)
        .map(
            bucketAt ->
                findTopByBucket(bucketGranularity, bucketAt, limit).stream()
                    .map(AlcoholPopularitySnapshot::getAlcoholId)
                    .toList())
        .orElseGet(List::of);
  }
}
