package app.bottlenote.alcohols.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 평가도 관측.
 *
 * <p>상태 축이다. 원본에 누적이 남아 있어 다음 회차가 현재 값을 다시 읽으면 저절로 정합된다.
 *
 * <p>평균이 아니라 합과 개수를 저장한다 — 평균만 저장하면 나중에 버킷을 롤업할 때 합칠 수 없다.
 */
@Entity(name = "alcohol_rating_observation")
@Table(
    name = "alcohol_rating_observations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_rating_obs_alcohol_bucket",
          columnNames = {"alcohol_id", "bucket_at"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholRatingObservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Comment("술 ID")
  @Column(name = "alcohol_id", nullable = false)
  private Long alcoholId;

  @Comment("관측 버킷 시각(정시 절삭)")
  @Column(name = "bucket_at", nullable = false)
  private LocalDateTime bucketAt;

  @Comment("이 축이 실제로 집계를 수행한 시각")
  @Column(name = "observed_at", nullable = false)
  private LocalDateTime observedAt;

  @Comment("직전 관측 버킷 시각")
  @Column(name = "prev_bucket_at")
  private LocalDateTime prevBucketAt;

  @Comment("누적 평점 수(0점 제외)")
  @Column(name = "rating_count", nullable = false)
  private Long ratingCount;

  @Comment("누적 평점 합")
  @Column(name = "rating_sum", nullable = false, precision = 12, scale = 1)
  private BigDecimal ratingSum;

  @Comment("직전 관측 대비 평점 수 증감")
  @Column(name = "delta_rating_count", nullable = false)
  private Long deltaRatingCount;

  @Comment("직전 관측 대비 평점 합 증감")
  @Column(name = "delta_rating_sum", nullable = false, precision = 12, scale = 1)
  private BigDecimal deltaRatingSum;

  @Comment("생성일시")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** 표본이 없으면 평균을 정의할 수 없으므로 0이 아니라 null을 준다. */
  public BigDecimal averageRating() {
    if (ratingCount == null || ratingCount == 0L || ratingSum == null) {
      return null;
    }
    return ratingSum.divide(BigDecimal.valueOf(ratingCount), 2, java.math.RoundingMode.HALF_UP);
  }

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}
