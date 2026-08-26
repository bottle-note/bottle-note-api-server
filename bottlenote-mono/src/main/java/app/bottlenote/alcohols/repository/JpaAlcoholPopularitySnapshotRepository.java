package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshot;
import app.bottlenote.alcohols.domain.AlcoholPopularitySnapshotRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaAlcoholPopularitySnapshotRepository
    extends AlcoholPopularitySnapshotRepository, JpaRepository<AlcoholPopularitySnapshot, Long> {

  @Override
  @Query(
      """
      select max(s.bucketAt) from alcohol_popularity_snapshot s
      where s.bucketGranularity = :bucketGranularity
      """)
  Optional<LocalDateTime> findLatestBucketAt(
      @Param("bucketGranularity") BucketGranularity bucketGranularity);

  @Query(
      """
      select s from alcohol_popularity_snapshot s
      where s.bucketGranularity = :bucketGranularity
        and s.bucketAt = :bucketAt
      order by s.popularityScore desc, s.alcoholId asc
      """)
  List<AlcoholPopularitySnapshot> queryTopByBucket(
      @Param("bucketGranularity") BucketGranularity bucketGranularity,
      @Param("bucketAt") LocalDateTime bucketAt,
      Pageable pageable);

  // 도메인 포트에 Pageable을 노출하지 않기 위해 여기서 감싼다
  @Override
  default List<AlcoholPopularitySnapshot> findTopByBucket(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt, int limit) {
    // Pageable.ofSize는 0 이하에서 예외를 던진다. 조회 파라미터가 그대로 흘러들 수 있으므로 여기서 막는다.
    if (limit <= 0) {
      return List.of();
    }
    return queryTopByBucket(bucketGranularity, bucketAt, Pageable.ofSize(limit));
  }

  @Query(
      """
      select s.alcoholId from alcohol_popularity_snapshot s
      join alcohol a on a.id = s.alcoholId
      where s.bucketGranularity = :bucketGranularity
        and a.deletedAt is null
        and s.bucketAt = (
          select max(s2.bucketAt) from alcohol_popularity_snapshot s2
          where s2.bucketGranularity = :bucketGranularity
        )
      order by s.popularityScore desc, s.alcoholId asc
      """)
  List<Long> queryLatestTopAlcoholIds(
      @Param("bucketGranularity") BucketGranularity bucketGranularity, Pageable pageable);

  @Override
  default List<Long> findLatestTopAlcoholIds(
      BucketGranularity bucketGranularity, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return queryLatestTopAlcoholIds(bucketGranularity, Pageable.ofSize(limit));
  }
}
