package app.bottlenote.support.report.dto.response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record DailyDataReportResponse(
    LocalDate reportDate,
    Long newUsersCount,
    Long newReviewsCount,
    Long newRepliesCount,
    Long newLikesCount,
    Long ratingEventsCount,
    Long newReportsCount,
    Long newInquiriesCount) {

  public boolean hasNewData() {
    return newUsersCount > 0
        || newReviewsCount > 0
        || newRepliesCount > 0
        || newLikesCount > 0
        || ratingEventsCount > 0
        || newReportsCount > 0
        || newInquiriesCount > 0;
  }

  public String toDiscordMessage() {
    String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    StringBuilder sb = new StringBuilder();
    sb.append("📊 **일일 데이터 리포트** - ").append(dateStr).append("\n\n");

    if (newUsersCount > 0) {
      sb.append("👥 **신규 유저**: ").append(newUsersCount).append("명\n");
    }
    if (newReviewsCount > 0) {
      sb.append("✍️ **신규 리뷰**: ").append(newReviewsCount).append("개\n");
    }
    if (newRepliesCount > 0) {
      sb.append("💬 **신규 댓글**: ").append(newRepliesCount).append("개\n");
    }
    if (newLikesCount > 0) {
      sb.append("❤️ **신규 좋아요**: ").append(newLikesCount).append("개\n");
    }
    sb.append("⭐ **별점 이벤트**: ").append(ratingEventsCount).append("건\n");
    if (newReportsCount > 0) {
      sb.append("🚨 **미처리 신고**: ").append(newReportsCount).append("건\n");
    }
    if (newInquiriesCount > 0) {
      sb.append("📧 **미처리 문의**: ").append(newInquiriesCount).append("건\n");
    }

    return sb.toString();
  }
}
