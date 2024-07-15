package app.bottlenote.review.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

public record SubReviewReplyInfo(
	Long userId,
	String imageUrl,
	String nickName,
	Long rootReviewId,
	Long parentReviewReplyId,
	String parentReviewReplyAuthor,
	Long reviewReplyId,
	String reviewReplyContent,
	LocalDateTime createAt
) {
	@Builder
	public SubReviewReplyInfo {
		parentReviewReplyAuthor = "@" + parentReviewReplyAuthor;
	}
}
