package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class ReviewDetail {

	private Long reviewId;
	private String reviewContent;
	private BigDecimal price;
	private SizeType sizeType;
	private Long likeCount;
	private Long replyCount;
	private String reviewImageUrl;
	private LocalDateTime createAt;

	private Long userId;
	private String nickName;
	private String userProfileImage;
	private Double rating;

	private ReviewStatus status;

	private Boolean isMyReview;
	private Boolean isLikedByMe;
	private Boolean hasReplyByMe;

	@Builder
	public ReviewDetail(
		Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType,
		Long likeCount, Long replyCount, String reviewImageUrl, LocalDateTime createAt,
		Long userId, String nickName, String userProfileImage, Double rating,
		ReviewStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe
	) {
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
