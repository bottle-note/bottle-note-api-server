package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 기간 단위별 인기도 관측과 Snapshot의 공통 조회 포트. */
public interface AlcoholPopularityBucketRepository<T> {

  T save(T entity);

  Optional<T> findByAlcoholIdAndBucketGranularityAndBucketAt(
      Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime bucketAt);

  List<T> findByBucketGranularityAndBucketAt(
      BucketGranularity bucketGranularity, LocalDateTime bucketAt);

  /** 시계열 조회 — 구간 경계는 양끝을 포함한다. */
  List<T> findByAlcoholIdAndBucketGranularityAndBucketAtBetweenOrderByBucketAtAsc(
      Long alcoholId, BucketGranularity bucketGranularity, LocalDateTime from, LocalDateTime to);
}
