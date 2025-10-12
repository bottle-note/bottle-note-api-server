package app.bottlenote.support.report.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "daily_data_report")
@Table(name = "daily_data_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class DailyDataReport extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("리포트 날짜")
  @Column(name = "report_date", nullable = false, unique = true)
  private LocalDate reportDate;

  @Comment("신규 유저 수")
  @Column(name = "new_users_count", nullable = false)
  @Builder.Default
  private Long newUsersCount = 0L;

  @Comment("신규 리뷰 수")
  @Column(name = "new_reviews_count", nullable = false)
  @Builder.Default
  private Long newReviewsCount = 0L;

  @Comment("신규 댓글 수")
  @Column(name = "new_replies_count", nullable = false)
  @Builder.Default
  private Long newRepliesCount = 0L;

  @Comment("신규 좋아요 수")
  @Column(name = "new_likes_count", nullable = false)
  @Builder.Default
  private Long newLikesCount = 0L;

  @Comment("웹훅 전송 여부")
  @Column(name = "webhook_sent", nullable = false)
  @Builder.Default
  private Boolean webhookSent = false;

  @Comment("웹훅 전송 시간")
  @Column(name = "webhook_sent_at")
  private LocalDateTime webhookSentAt;

  public void markWebhookSent() {
    this.webhookSent = true;
    this.webhookSentAt = LocalDateTime.now();
  }
}
