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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 버킷별 최종 인기도.
 *
 * <p>관측 테이블과 달리 조밀하다 — 조회 기준이자 정렬 대상이라 매 버킷 대상 전체를 적재한다.
 *
 * <p>축별 sourceBucketAt은 그 값이 실제로 관측된 버킷이다. bucketAt과의 차이가 곧 갭이며, 이것이 없으면 끌어온 값이 얼마나 묵은 것인지 알 수 없다.
 */
@Entity(name = "alcohol_popularity_snapshot")
@Table(
    name = "alcohol_popularity_snapshots",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_popularity_snapshot_granularity_bucket_alcohol",
          columnNames = {"bucket_granularity", "bucket_at", "alcohol_id"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholPopularitySnapshot {

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

  @Comment("Batch가 Snapshot 계산을 완료한 시각")
  @Column(name = "observed_at", nullable = false)
  private LocalDateTime observedAt;

  @Comment("같은 기간 단위에서 직전 Snapshot의 기간 시작 시각")
  @Column(name = "prev_bucket_at")
  private LocalDateTime prevBucketAt;

  @Comment("관심도 원시값")
  @Column(name = "interest_value", nullable = false)
  private Long interestValue;

  @Comment("관심도 값이 실제 관측된 버킷")
  @Column(name = "interest_source_bucket_at")
  private LocalDateTime interestSourceBucketAt;

  @Comment("관심도 정규화 점수(0~1)")
  @Column(name = "interest_score", nullable = false, precision = 6, scale = 4)
  private BigDecimal interestScore;

  @Comment("평가도 원시값")
  @Column(name = "rating_value", nullable = false)
  private Long ratingValue;

  @Comment("평가도 값이 실제 관측된 버킷")
  @Column(name = "rating_source_bucket_at")
  private LocalDateTime ratingSourceBucketAt;

  @Comment("평가도 정규화 점수(0~1)")
  @Column(name = "rating_score", nullable = false, precision = 6, scale = 4)
  private BigDecimal ratingScore;

  @Comment("선호도 원시값")
  @Column(name = "pick_value", nullable = false)
  private Long pickValue;

  @Comment("선호도 값이 실제 관측된 버킷")
  @Column(name = "pick_source_bucket_at")
  private LocalDateTime pickSourceBucketAt;

  @Comment("선호도 정규화 점수(0~1)")
  @Column(name = "pick_score", nullable = false, precision = 6, scale = 4)
  private BigDecimal pickScore;

  @Comment("참여도 원시값")
  @Column(name = "engagement_value", nullable = false)
  private Long engagementValue;

  @Comment("참여도 값이 실제 관측된 버킷")
  @Column(name = "engagement_source_bucket_at")
  private LocalDateTime engagementSourceBucketAt;

  @Comment("참여도 정규화 점수(0~1)")
  @Column(name = "engagement_score", nullable = false, precision = 6, scale = 4)
  private BigDecimal engagementScore;

  @Comment("가중 합산한 최종 인기도(0~1)")
  @Column(name = "popularity_score", nullable = false, precision = 6, scale = 4)
  private BigDecimal popularityScore;

  @Comment("생성일시")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** 해당 축 값이 몇 분 전 관측인지. 관측 이력이 아예 없으면 null이다. */
  public Long interestGapMinutes() {
    return gapMinutes(interestSourceBucketAt);
  }

  public Long ratingGapMinutes() {
    return gapMinutes(ratingSourceBucketAt);
  }

  public Long pickGapMinutes() {
    return gapMinutes(pickSourceBucketAt);
  }

  public Long engagementGapMinutes() {
    return gapMinutes(engagementSourceBucketAt);
  }

  private Long gapMinutes(LocalDateTime sourceBucketAt) {
    if (sourceBucketAt == null || bucketAt == null) {
      return null;
    }
    return Duration.between(sourceBucketAt, bucketAt).toMinutes();
  }

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}
