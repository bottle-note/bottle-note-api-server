package app.bottlenote.review.dto.vo;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReviewInfo(
	Long reviewId,
	String reviewContent,
	BigDecimal price,
	SizeType sizeType,
	Long likeCount,
	Long replyCount,
	UserInfo userInfo,
	String reviewImageUrl,
	Double rating,
	Long viewCount,
	LocationInfo locationInfo,
	ReviewDisplayStatus status,
	Boolean isMyReview,
	Boolean isLikedByMe,
	Boolean hasReplyByMe,
	Boolean isBestReview,
	List<String> reviewTastingTag,
	LocalDateTime createAt
) {
}
