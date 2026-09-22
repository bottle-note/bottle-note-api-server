package app.bottlenote.review.domain;

import app.bottlenote.review.constant.BestReviewChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 베스트 리뷰 선정 변동 로그.
 *
 * <p>일간 배치가 리뷰의 is_best를 바꿀 때 왜 바뀌었는지를 판정 시점 수치와 함께 남긴다. 쓰기는 배치가 JDBC로 하고, 이 엔티티는 관리자 조회와 스키마 검증에
 * 쓴다.
 */
@Entity(name = "best_review_selection_log")
@Table(
    name = "best_review_selection_logs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_best_review_selection_logs_date_review_type",
          columnNames = {"selection_date", "review_id", "change_type"})
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BestReviewSelectionLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Comment("선정 배치 기준일")
  @Column(name = "selection_date", nullable = false)
  private LocalDate selectionDate;

  @Comment("대상 리뷰 ID")
  @Column(name = "review_id", nullable = false)
  private Long reviewId;

  @Comment("리뷰가 속한 주류 ID")
  @Column(name = "alcohol_id", nullable = false)
  private Long alcoholId;

  @Comment("선정 또는 해제")
  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 16)
  private BestReviewChangeType changeType;

  @Comment("주류 안에서의 점수 순위")
  @Column(name = "ranking")
  private Integer ranking;

  @Comment("선정 점수")
  @Column(name = "score", nullable = false, precision = 8, scale = 2)
  private BigDecimal score;

  @Comment("판정 시점 LIKE 수")
  @Column(name = "like_count", nullable = false)
  private Long likeCount;

  @Comment("판정 시점 DISLIKE 수")
  @Column(name = "dislike_count", nullable = false)
  private Long dislikeCount;

  @Comment("판정 시점 작성자 본인을 뺀 NORMAL 댓글 작성자 수")
  @Column(name = "reply_count", nullable = false)
  private Long replyCount;

  @Comment("판정 시점 이미지 수")
  @Column(name = "image_count", nullable = false)
  private Long imageCount;

  @Comment("판정 시점 본문 글자 수")
  @Column(name = "content_length", nullable = false)
  private Integer contentLength;

  @Comment("판정 시점 같은 주류의 ACTIVE·PUBLIC 리뷰 수")
  @Column(name = "alcohol_review_count", nullable = false)
  private Long alcoholReviewCount;

  @Comment("적용된 선정 규칙 코드")
  @Column(name = "rule_code", nullable = false, length = 64)
  private String ruleCode;

  @Comment("사람이 읽는 선정·해제 사유")
  @Column(name = "reason", nullable = false, length = 500)
  private String reason;

  @Comment("로그 생성 시각")
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;
}
