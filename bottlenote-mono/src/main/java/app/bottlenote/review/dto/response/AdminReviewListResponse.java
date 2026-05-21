package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import java.time.LocalDateTime;

public record AdminReviewListResponse(
    Long reviewId,
    Long alcoholId,
    String alcoholName,
    Long userId,
    String userNickname,
    String content,
    Double reviewRating,
    ReviewActiveStatus activeStatus,
    ReviewDisplayStatus displayStatus,
    Long replyCount,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt) {}
