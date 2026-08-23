package app.bottlenote.alcohols.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 참여도 관측.
 *
 * <p>상태 축이다. 리뷰 자체와 리뷰에 붙는 2차 반응(좋아요·댓글)을 함께 센다.
 *
 * <p>좋아요는 LIKE와 DISLIKE를 분리해 센다 — 취소가 삭제가 아니라 상태 전이라서 합쳐 세면 철회가 참여로 집계된다.
 */
@Entity(name = "alcohol_engagement_observation")
@Table(
    name = "alcohol_engagement_observations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_engagement_obs_alcohol_bucket",
          columnNames = {"alcohol_id", "bucket_at"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholEngagementObservation {

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

  @Comment("누적 리뷰 수(ACTIVE + PUBLIC)")
  @Column(name = "review_count", nullable = false)
  private Long reviewCount;

  @Comment("누적 좋아요 수(LIKE 상태만)")
  @Column(name = "like_count", nullable = false)
  private Long likeCount;

  @Comment("누적 취소 수(DISLIKE 상태)")
  @Column(name = "dislike_count", nullable = false)
  private Long dislikeCount;

  @Comment("누적 댓글 수(NORMAL 상태만)")
  @Column(name = "reply_count", nullable = false)
  private Long replyCount;

  @Comment("직전 관측 대비 리뷰 증감")
  @Column(name = "delta_review_count", nullable = false)
  private Long deltaReviewCount;

  @Comment("직전 관측 대비 좋아요 증감")
  @Column(name = "delta_like_count", nullable = false)
  private Long deltaLikeCount;

  @Comment("직전 관측 대비 취소 증감")
  @Column(name = "delta_dislike_count", nullable = false)
  private Long deltaDislikeCount;

  @Comment("직전 관측 대비 댓글 증감")
  @Column(name = "delta_reply_count", nullable = false)
  private Long deltaReplyCount;

  @Comment("생성일시")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 참여도 대표값. 최종 적재에서 정규화 입력으로 쓴다.
   *
   * <p>DISLIKE는 철회 신호이므로 더하지 않는다. 감점 여부는 산식이 확정된 뒤 결정한다.
   */
  public long engagementValue() {
    return nz(reviewCount) + nz(likeCount) + nz(replyCount);
  }

  private static long nz(Long value) {
    return value == null ? 0L : value;
  }

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}
