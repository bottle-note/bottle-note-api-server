package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReviewResponse {

	private final Long reviewId;
	private final String reviewContent;
	private final BigDecimal price;
	private final SizeType sizeType;
	private final Long likeCount;
	private final Long replyCount;
	private final String reviewImageUrl;
	private final LocalDateTime createAt;

	private final Long userId;
	private final String nickName;
	private final String userProfileImage;
	private final Double rating;

	private final ReviewDisplayStatus status;

	private final Boolean isMyReview;
	private final Boolean isLikedByMe;
	private final Boolean hasReplyByMe;

	@Builder
	public ReviewResponse(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType, Long likeCount, Long replyCount, String reviewImageUrl, LocalDateTime createAt, Long userId, String nickName, String userProfileImage,
		Double rating, ReviewDisplayStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe) {
		this.reviewId = reviewId;
		this.reviewContent = reviewContent;
		this.price = price;
		this.sizeType = sizeType;
		this.likeCount = likeCount;
		this.replyCount = replyCount;
		this.reviewImageUrl = reviewImageUrl;
		this.createAt = createAt;
		this.userId = userId;
		this.nickName = nickName;
		this.userProfileImage = userProfileImage;
		this.rating = rating;
		this.status = status;
		this.isMyReview = isMyReview;
		this.isLikedByMe = isLikedByMe;
		this.hasReplyByMe = hasReplyByMe;
	}
}
