package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.BestReviewChangeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminBestReviewSelectionLogResponse(
    Long logId,
    LocalDate selectionDate,
    Long reviewId,
    Long alcoholId,
    String alcoholName,
    Long userId,
    String userNickname,
    BestReviewChangeType changeType,
    Integer ranking,
    BigDecimal score,
    Long likeCount,
    Long dislikeCount,
    Long replyCount,
    Long imageCount,
    Integer contentLength,
    Long alcoholReviewCount,
    String ruleCode,
    String reason,
    LocalDateTime createdAt) {}
