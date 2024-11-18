package app.bottlenote.review.dto.vo;

import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
	ReviewLocation locationInfo,

	ReviewDisplayStatus status,

	Boolean isMyReview,
	Boolean isLikedByMe,
	Boolean hasReplyByMe,
	Boolean isBestReview,
	String tastingTagList,
	LocalDateTime createAt
) {
}
