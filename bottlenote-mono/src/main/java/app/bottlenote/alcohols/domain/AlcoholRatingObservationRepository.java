package app.bottlenote.alcohols.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholRatingObservationRepository {

  AlcoholRatingObservation save(AlcoholRatingObservation entity);

  Optional<AlcoholRatingObservation> findByAlcoholIdAndBucketAt(
      Long alcoholId, LocalDateTime bucketAt);

  List<AlcoholRatingObservation> findByBucketAt(LocalDateTime bucketAt);

  /** 어드민 시계열 조회 — 구간 경계는 양끝을 포함한다. */
  List<AlcoholRatingObservation> findByAlcoholIdAndBucketAtBetweenOrderByBucketAtAsc(
      Long alcoholId, LocalDateTime from, LocalDateTime to);
}
