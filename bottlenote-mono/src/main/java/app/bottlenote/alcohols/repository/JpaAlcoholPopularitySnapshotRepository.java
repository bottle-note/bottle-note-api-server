package app.bottlenote.alcohols.repository;

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
  @Query("select max(s.bucketAt) from alcohol_popularity_snapshot s")
  Optional<LocalDateTime> findLatestBucketAt();

  @Query(
      """
      select s from alcohol_popularity_snapshot s
      where s.bucketAt = :bucketAt
      order by s.popularityScore desc, s.alcoholId asc
      """)
  List<AlcoholPopularitySnapshot> queryTopByBucketAt(
      @Param("bucketAt") LocalDateTime bucketAt, Pageable pageable);

  // 도메인 포트에 Pageable을 노출하지 않기 위해 여기서 감싼다
  @Override
  default List<AlcoholPopularitySnapshot> findTopByBucketAt(LocalDateTime bucketAt, int limit) {
    // Pageable.ofSize는 0 이하에서 예외를 던진다. 조회 파라미터가 그대로 흘러들 수 있으므로 여기서 막는다.
    if (limit <= 0) {
      return List.of();
    }
    return queryTopByBucketAt(bucketAt, Pageable.ofSize(limit));
  }
}
