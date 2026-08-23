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
public interface AlcoholEngagementObservationRepository {

  AlcoholEngagementObservation save(AlcoholEngagementObservation entity);

  Optional<AlcoholEngagementObservation> findByAlcoholIdAndBucketAt(
      Long alcoholId, LocalDateTime bucketAt);

  List<AlcoholEngagementObservation> findByBucketAt(LocalDateTime bucketAt);

  /** 어드민 시계열 조회 — 구간 경계는 양끝을 포함한다. */
  List<AlcoholEngagementObservation> findByAlcoholIdAndBucketAtBetweenOrderByBucketAtAsc(
      Long alcoholId, LocalDateTime from, LocalDateTime to);
}
