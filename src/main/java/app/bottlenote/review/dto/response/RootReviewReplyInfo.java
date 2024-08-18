package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewReplyStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record RootReviewReplyInfo(

	Long totalCount,
	List<Info> reviewReplies
) {
	public static RootReviewReplyInfo of(Long totalCount, List<Info> reviewReplyList) {
		return new RootReviewReplyInfo(totalCount, reviewReplyList);
	}

	public record Info(
		Long userId,
		String imageUrl,
		String nickName,
		Long reviewReplyId,
		String reviewReplyContent,
		Long subReplyCount,
		ReviewReplyStatus status,
		LocalDateTime createAt
	) {
		@Builder
		public Info {
		}
	}
}
