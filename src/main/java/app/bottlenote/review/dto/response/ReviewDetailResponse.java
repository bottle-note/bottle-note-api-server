package app.bottlenote.review.dto.response;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public record ReviewDetailResponse(
	AlcoholInfo alcoholInfo,

	ReviewInfo reviewResponse,

	List<ReviewImageInfo> reviewImageList
) {

	@Builder
	public record ReviewInfo(
		Long reviewId,
		String reviewContent,
		BigDecimal price,
		SizeType sizeType,
		Long likeCount,
		Long replyCount,
		String reviewImageUrl,
		LocalDateTime createAt,

		Long userId,
		String nickName,
		String userProfileImage,
		Double rating,

		String zipCode,
		String address,
		String detailAddress,

		ReviewDisplayStatus status,

		Boolean isMyReview,
		Boolean isLikedByMe,
		Boolean hasReplyByMe,
		Boolean isBestReview,
		List<String> reviewTastingTag

	) {
	}

	public static ReviewDetailResponse create(AlcoholInfo alcoholInfo, ReviewInfo reviewResponse, List<ReviewImageInfo> reviewImageList) {
		return new ReviewDetailResponse(alcoholInfo, reviewResponse, reviewImageList);
	}
}

