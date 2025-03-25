package app.bottlenote.review.dto.response;

import app.bottlenote.review.constant.ReviewReplyStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record RootReviewReplyResponse(
		Long totalCount,
		List<Item> reviewReplies
) {
	public static RootReviewReplyResponse of(Long totalCount, List<Item> reviewReplyList) {
		return new RootReviewReplyResponse(totalCount, reviewReplyList);
	}

	@Builder
	public record Item(
			Long userId,
			String imageUrl,
			String nickName,
			Long reviewReplyId,
			String reviewReplyContent,
			Long subReplyCount,
			ReviewReplyStatus status,
			LocalDateTime createAt
	) {
	}
}
