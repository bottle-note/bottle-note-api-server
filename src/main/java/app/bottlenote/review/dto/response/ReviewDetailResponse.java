package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class ReviewDetailResponse extends ReviewResponse {

	private AlcoholInfo alcoholInfo;

	private List<ReviewImageInfo> reviewImageList;

	private List<ReviewReplyInfo> reviewReplyList;

	@Builder
	public ReviewDetailResponse(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType, Long likeCount, Long replyCount, String reviewImageUrl, LocalDateTime createAt, Long userId, String nickName, String userProfileImage,
		Double rating, String zipCode, String address, String detailAddress, ReviewDisplayStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe, AlcoholInfo alcoholInfo, List<ReviewImageInfo> reviewImageList,
		List<ReviewReplyInfo> reviewReplyList) {
		super(reviewId, reviewContent, price, sizeType, likeCount, replyCount, reviewImageUrl, createAt, userId, nickName, userProfileImage, rating, zipCode, address, detailAddress, status, isMyReview, isLikedByMe, hasReplyByMe);
		this.alcoholInfo = alcoholInfo;
		this.reviewImageList = reviewImageList;
		this.reviewReplyList = reviewReplyList;
	}

	public void updateAlcoholInfo(AlcoholInfo alcoholInfo) {
		this.alcoholInfo = alcoholInfo;
	}

	public void updateReviewImageList(List<ReviewImageInfo> reviewImageInfo) {
		this.reviewImageList = reviewImageInfo;
	}

	public void updateReviewReplyList(List<ReviewReplyInfo> reviewReplyList) {
		this.reviewReplyList = reviewReplyList;
	}
}
