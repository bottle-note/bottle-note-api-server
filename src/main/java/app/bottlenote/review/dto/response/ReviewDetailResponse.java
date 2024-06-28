package app.bottlenote.review.dto.response;

import app.bottlenote.review.dto.request.ReviewImageInfo;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReviewDetailResponse extends ReviewResponse {

	private AlcoholInfo alcoholInfo;

	private List<ReviewImageInfo> reviewImageList;

	private List<ReviewReplyInfo> reviewReplyList;

	public ReviewDetailResponse(ReviewResponse reviewResponse) {
		super(reviewResponse.getReviewId(), reviewResponse.getReviewContent(), reviewResponse.getPrice(), reviewResponse.getSizeType(),
			reviewResponse.getLikeCount(), reviewResponse.getReplyCount(), reviewResponse.getReviewImageUrl(), reviewResponse.getCreateAt(),
			reviewResponse.getUserId(), reviewResponse.getNickName(), reviewResponse.getUserProfileImage(), reviewResponse.getRating(),
			reviewResponse.getZipCode(), reviewResponse.getAddress(), reviewResponse.getDetailAddress(), reviewResponse.getStatus(),
			reviewResponse.getIsMyReview(), reviewResponse.getIsLikedByMe(), reviewResponse.getHasReplyByMe());
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
