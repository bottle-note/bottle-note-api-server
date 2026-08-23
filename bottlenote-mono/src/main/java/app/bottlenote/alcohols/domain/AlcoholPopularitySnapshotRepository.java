package app.bottlenote.alcohols.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 관측 결과 조회용 포트.
 *
 * <p>쓰기 주체는 배치의 upsert SQL 하나뿐이다. 같은 (alcohol_id, bucket_at)에 이미 행이 있으면 JPA save는 유니크 키 위반이 나므로,
 * 여기의 save는 최초 삽입 경로에만 유효하다. 재적재 멱등성은 배치 SQL이 책임진다.
 */
@DomainRepository
public interface AlcoholPopularitySnapshotRepository {

  AlcoholPopularitySnapshot save(AlcoholPopularitySnapshot entity);

  /**
   * 전역 최신 버킷 하나를 고른다.
   *
   * <p>주류별로 각자의 최신을 고르면 서로 다른 시점의 값이 한 목록에 섞여 정렬이 의미를 잃는다.
   */
  Optional<LocalDateTime> findLatestBucketAt();

  /** 지정한 버킷 안에서만 점수순으로 상위를 고른다. */
  List<AlcoholPopularitySnapshot> findTopByBucketAt(LocalDateTime bucketAt, int limit);

  /** 어드민 시계열 조회 — 구간 경계는 양끝을 포함한다. */
  List<AlcoholPopularitySnapshot> findByAlcoholIdAndBucketAtBetweenOrderByBucketAtAsc(
      Long alcoholId, LocalDateTime from, LocalDateTime to);
}
