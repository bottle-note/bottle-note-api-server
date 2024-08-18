package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewReplyStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public record SubReviewReplyInfo(
	Long totalCount,
	List<Info> reviewReplies
) {

	public static SubReviewReplyInfo of(Long totalCount, List<Info> reviewReplays) {
		return new SubReviewReplyInfo(totalCount, reviewReplays);
	}

	public record Info(
		Long userId,
		String imageUrl,
		String nickName,
		Long rootReviewId,
		Long parentReviewReplyId,
		String parentReviewReplyAuthor,
		Long reviewReplyId,
		String reviewReplyContent,
		ReviewReplyStatus status,
		LocalDateTime createAt
	) {
		@Builder
		public Info {
			parentReviewReplyAuthor = "@" + parentReviewReplyAuthor;
		}
	}
}
