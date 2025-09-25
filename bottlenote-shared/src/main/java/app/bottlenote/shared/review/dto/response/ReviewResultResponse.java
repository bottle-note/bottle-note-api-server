package app.bottlenote.shared.review.dto.response;

import app.bottlenote.shared.review.constant.ReviewResultMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReviewResultResponse(
    ReviewResultMessage codeMessage, String message, Long reviewId, String responseAt) {

  public static ReviewResultResponse response(ReviewResultMessage message, Long reviewId) {
    return new ReviewResultResponse(
        message,
        message.getDescription(),
        reviewId,
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
