package app.bottlenote.alcohols.dto.response;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Getter
public class AlcoholDetail {

	private List<ReviewOfAlcoholDetail> bestReviews;
	private List<ReviewOfAlcoholDetail> reviews;

	public AlcoholDetail() {
	}

	@Builder
	public AlcoholDetail(List<ReviewOfAlcoholDetail> bestReviews, List<ReviewOfAlcoholDetail> reviews) {
		this.bestReviews = bestReviews;
		this.reviews = reviews;
	}

	@Getter
	@AllArgsConstructor
	public static class ReviewOfAlcoholDetail {
		private final Long userId;
		private final String imageUrl;
		private final String nickName;
		private final Long reviewId;
		private final String reviewContent;
		private final Double rating;
		private final SizeType sizeType;
		private final BigDecimal price;
		private final Long viewCount;
		private final Long likeCount;
		private final Boolean isMyLike;
		private final Long replyCount;
		private final Boolean isMyReply;
		private final ReviewStatus status;
		private final String reviewImageUrl;
		private final LocalDateTime createAt;
	}
}
