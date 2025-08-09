package app.bottlenote.support.report.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.report.constant.ReviewReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "review_report")
@Table(name = "review_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ReviewReport extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("유저 ID")
  @Column(name = "user_id")
  private Long userId;

  @Comment("리뷰 ID")
  @Column(name = "review_id")
  private Long reviewId;

  @Comment("신고 사유")
  @Column(name = "report_content", nullable = false)
  private String reportContent;

  @Enumerated(EnumType.STRING)
  @Comment("리뷰 신고 타입")
  @Column(name = "type", nullable = false)
  private ReviewReportType type;

  @Comment("신고자 IP주소")
  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  @Comment("문의글의 처리 상태 : Wating이 디폴트")
  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  @Builder.Default
  private StatusType status = StatusType.WAITING;

  @Comment("관리자 ID")
  @Column(name = "admin_id")
  private Long adminId;

  @Comment("처리 결과")
  @Column(name = "response_content", nullable = false)
  private String responseContent;

  public static ReviewReport registerReport(
      Long userId, Long reviewId, ReviewReportType type, String reportContent, String ipAddress) {
    return ReviewReport.builder()
        .userId(userId)
        .reviewId(reviewId)
        .type(type)
        .reportContent(reportContent)
        .ipAddress(ipAddress)
        .build();
  }
}
