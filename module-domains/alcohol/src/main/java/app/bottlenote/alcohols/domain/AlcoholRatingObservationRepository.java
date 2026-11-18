package app.bottlenote.alcohols.domain;

import app.bottlenote.common.annotation.DomainRepository;

/**
 * 관측 결과 조회용 포트.
 *
 * <p>쓰기 주체는 배치의 upsert SQL 하나뿐이다. 같은 (alcohol_id, bucket_granularity, bucket_at)에 이미 행이 있으면 JPA
 * save는 유니크 키 위반이 나므로, 여기의 save는 최초 삽입 경로에만 유효하다. 재적재 멱등성은 배치 SQL이 책임진다.
 */
@DomainRepository
public interface AlcoholRatingObservationRepository
    extends AlcoholPopularityBucketRepository<AlcoholRatingObservation> {}
