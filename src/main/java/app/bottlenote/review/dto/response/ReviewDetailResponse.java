package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public record ReviewDetailResponse(
	AlcoholInfo alcoholInfo,

	ReviewDetailInfo reviewResponse,

	List<ReviewImageInfo> reviewImageList
) {

	@Getter
	public static class ReviewDetailInfo {

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

		private final String zipCode;
		private final String address;
		private final String detailAddress;

		private final ReviewDisplayStatus status;

		private final Boolean isMyReview;
		private final Boolean isLikedByMe;
		private final Boolean hasReplyByMe;
		private final Boolean isBestReview;
		private List<String> reviewTastingTag;

		@Builder
		public ReviewDetailInfo(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType, Long likeCount, Long replyCount, String reviewImageUrl, LocalDateTime createAt, Long userId, String nickName, String userProfileImage,
			Double rating, String zipCode, String address, String detailAddress, ReviewDisplayStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe, Boolean isBestReview) {
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
			this.zipCode = zipCode;
			this.address = address;
			this.detailAddress = detailAddress;
			this.status = status;
			this.isMyReview = isMyReview;
			this.isLikedByMe = isLikedByMe;
			this.hasReplyByMe = hasReplyByMe;
			this.isBestReview = isBestReview;
		}

		public void updateTastingTagList(List<String> reviewTastingTag) {
			this.reviewTastingTag = reviewTastingTag;
		}

	}

	public static ReviewDetailResponse create(AlcoholInfo alcoholInfo, ReviewDetailInfo reviewResponse, List<ReviewImageInfo> reviewImageList) {
		return new ReviewDetailResponse(alcoholInfo, reviewResponse, reviewImageList);
	}
}

