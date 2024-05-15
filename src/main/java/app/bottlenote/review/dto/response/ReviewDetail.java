package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReviewDetail {

	private Long reviewId;
	private String reviewContent;
	private BigDecimal price;
	private SizeType sizeType;
	private Long likeCount;
	private Long replyCount;
	private String thumbnailImage;
	private LocalDateTime reviewCreatedAt;

	private Long userId;
	private String userNickname;
	private String userProfileImage;
	private Double ratingPoint;

	private ReviewStatus status;

	private Boolean isMyReview;
	private Boolean isLikedByMe;
	private Boolean hasCommentedByMe;

	@Builder
	public ReviewDetail(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType,
		Long likeCount, Long replyCount, String thumbnailImage, LocalDateTime reviewCreatedAt,
		Long userId, String userNickname, String userProfileImage, Double ratingPoint,
		ReviewStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasCommentedByMe) {
		this.reviewId = reviewId;
		this.reviewContent = reviewContent;
		this.price = price;
		this.sizeType = sizeType;
		this.likeCount = likeCount;
		this.replyCount = replyCount;
		this.thumbnailImage = thumbnailImage;
		this.reviewCreatedAt = reviewCreatedAt;
		this.userId = userId;
		this.userNickname = userNickname;
		this.userProfileImage = userProfileImage;
		this.ratingPoint = ratingPoint;
		this.status = status;
		this.isMyReview = isMyReview;
		this.isLikedByMe = isLikedByMe;
		this.hasCommentedByMe = hasCommentedByMe;
	}

	public ReviewDetail() {
	}
}
