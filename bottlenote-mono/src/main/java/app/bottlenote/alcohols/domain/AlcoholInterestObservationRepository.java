package app.bottlenote.alcohols.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface AlcoholInterestObservationRepository {

  AlcoholInterestObservation save(AlcoholInterestObservation entity);

  Optional<AlcoholInterestObservation> findByAlcoholIdAndBucketAt(
      Long alcoholId, LocalDateTime bucketAt);

  List<AlcoholInterestObservation> findByBucketAt(LocalDateTime bucketAt);

  /** 어드민 시계열 조회 — 구간 경계는 양끝을 포함한다. */
  List<AlcoholInterestObservation> findByAlcoholIdAndBucketAtBetweenOrderByBucketAtAsc(
      Long alcoholId, LocalDateTime from, LocalDateTime to);
}
