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
 * 선호도 관측.
 *
 * <p>상태 축이다. 픽 취소가 행 삭제가 아니라 상태 전이라서 PICK만 세며, 철회 신호를 따로 보기 위해 UNPICK도 함께 센다.
 */
@Entity(name = "alcohol_pick_observation")
@Table(
    name = "alcohol_pick_observations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_pick_obs_alcohol_bucket",
          columnNames = {"alcohol_id", "bucket_at"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholPickObservation {

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

  @Comment("현재 PICK 상태 수")
  @Column(name = "pick_count", nullable = false)
  private Long pickCount;

  @Comment("현재 UNPICK 상태 수")
  @Column(name = "unpick_count", nullable = false)
  private Long unpickCount;

  // 픽은 철회될 수 있으므로 증감이 음수일 수 있다
  @Comment("직전 관측 대비 PICK 증감")
  @Column(name = "delta_pick_count", nullable = false)
  private Long deltaPickCount;

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
