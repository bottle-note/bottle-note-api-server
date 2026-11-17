package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.BucketGranularity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 관심도 관측.
 *
 * <p>흐름 축이다. 원본에 누적 조회수가 없어 관측 시점에 구간을 세는 방식이라, 세지 않고 지나간 구간은 복구되지 않는다.
 */
@Entity(name = "alcohol_interest_observation")
@Table(
    name = "alcohol_interest_observations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_interest_obs_granularity_bucket_alcohol",
          columnNames = {"bucket_granularity", "bucket_at", "alcohol_id"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholInterestObservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Comment("술 ID")
  @Column(name = "alcohol_id", nullable = false)
  private Long alcoholId;

  @Comment("기간 단위")
  @Enumerated(EnumType.STRING)
  @Column(name = "bucket_granularity", nullable = false, length = 8)
  private BucketGranularity bucketGranularity;

  @Comment("기간 시작 시각")
  @Column(name = "bucket_at", nullable = false)
  private LocalDateTime bucketAt;

  @Comment("이 축이 실제로 집계를 수행한 시각")
  @Column(name = "observed_at", nullable = false)
  private LocalDateTime observedAt;

  // 희소 저장이라 직전 관측이 바로 앞 버킷이 아닐 수 있다
  @Comment("직전 관측 버킷 시각")
  @Column(name = "prev_bucket_at")
  private LocalDateTime prevBucketAt;

  @Comment("해당 기간의 주류 상세 조회 요청 수")
  @Column(name = "view_count", nullable = false)
  private Long viewCount;

  @Comment("관측 시작부터 해당 기간 종료까지의 누적 상세 조회 요청 수")
  @Column(name = "cumulative_view_count", nullable = false)
  private Long cumulativeViewCount;

  @Comment("생성일시")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}
