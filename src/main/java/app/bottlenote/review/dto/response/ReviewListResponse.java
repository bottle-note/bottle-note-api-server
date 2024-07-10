package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ReviewListResponse {

	private final Long totalCount;
	private final List<ReviewInfo> reviewList;

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

		ReviewDisplayStatus status,

		Boolean isMyReview,
		Boolean isLikedByMe,
		Boolean hasReplyByMe
	) {

	}


}
