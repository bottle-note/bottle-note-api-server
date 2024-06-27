package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class ReviewsDetailInfo {
	private Long totalReviewCount;
	private List<ReviewInfo> bestReviewInfos;
	private List<ReviewInfo> recentReviewInfos;

	@Builder
	public ReviewsDetailInfo(
		Long totalReviewCount,
		List<ReviewInfo> bestReviewInfos,
		List<ReviewInfo> recentReviewInfos
	) {
		this.totalReviewCount = totalReviewCount;
		this.bestReviewInfos = bestReviewInfos;
		this.recentReviewInfos = recentReviewInfos;
	}

	@Builder
	public record ReviewInfo(
		Long userId,
		String imageUrl,
		String nickName,
		Long reviewId,
		String reviewContent,
		Double rating,
		SizeType sizeType,
		BigDecimal price,
		Long viewCount,
		Long likeCount,
		Boolean isLikedByMe,
		Long replyCount,
		Boolean hasReplyByMe,
		ReviewDisplayStatus status,
		String reviewImageUrl,
		LocalDateTime createAt
	) {
	}
}
