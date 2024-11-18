package app.bottlenote.review.dto.common;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record CommonReviewInfo(
	Long reviewId,
	String reviewContent,
	BigDecimal price,
	SizeType sizeType,
	Long likeCount,
	Long replyCount,
	String reviewImageUrl,

	UserInfo userInfo,

	Double rating,
	Long viewCount,
	LocationInfo locationInfo,
	ReviewDisplayStatus status,
	Boolean isMyReview,
	Boolean isLikedByMe,
	Boolean hasReplyByMe,
	Boolean isBestReview,
	List<String> reviewTastingTag,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	LocalDateTime createAt
) {

	@Builder
	public CommonReviewInfo(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType, Long likeCount, Long replyCount, String reviewImageUrl, UserInfo userInfo, Double rating, Long viewCount, LocationInfo locationInfo, ReviewDisplayStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe, Boolean isBestReview, List<String> reviewTastingTag, LocalDateTime createAt) {
		this.reviewId = reviewId;
		this.reviewContent = reviewContent;
		this.price = price;
		this.sizeType = sizeType;
		this.likeCount = likeCount;
		this.replyCount = replyCount;
		this.reviewImageUrl = reviewImageUrl;
		this.userInfo = userInfo;
		this.rating = rating;
		this.viewCount = viewCount;
		this.locationInfo = locationInfo;
		this.status = status;
		this.isMyReview = isMyReview;
		this.isLikedByMe = isLikedByMe;
		this.hasReplyByMe = hasReplyByMe;
		this.isBestReview = isBestReview;
		this.reviewTastingTag = reviewTastingTag;
		this.createAt = createAt;
	}
}
