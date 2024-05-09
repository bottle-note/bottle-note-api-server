package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@NoArgsConstructor
public class ReviewsDetailInfo {

	private List<ReviewInfo> bestReviewInfos;
	private List<ReviewInfo> recentReviewInfos;

	@Builder
	public ReviewsDetailInfo(List<ReviewInfo> bestReviewInfos, List<ReviewInfo> recentReviewInfos) {
		this.bestReviewInfos = bestReviewInfos;
		this.recentReviewInfos = recentReviewInfos;
	}

	public record ReviewInfo(Long userId,
							 String imageUrl,
							 String nickName,
							 Long reviewId,
							 String reviewContent,
							 Double rating,
							 SizeType sizeType,
							 BigDecimal price,
							 Long viewCount,
							 Long likeCount,
							 Boolean isMyLike,
							 Long replyCount,
							 Boolean isMyReply,
							 ReviewStatus status,
							 String reviewImageUrl,
							 LocalDateTime createAt) {
	}
}
