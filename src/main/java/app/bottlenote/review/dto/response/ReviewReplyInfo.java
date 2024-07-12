package app.bottlenote.review.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

public record ReviewReplyInfo(
	Long userId,
	String imageUrl,
	String nickName,
	Long reviewReplyId,
	String reviewReplyContent,
	LocalDateTime createAt
) {
	@Builder
	public ReviewReplyInfo {
	}
}
