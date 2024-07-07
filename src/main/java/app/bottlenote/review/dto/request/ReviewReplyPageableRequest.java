package app.bottlenote.review.dto.request;

import lombok.Builder;

/**
 * 리뷰 댓글 조회 시 사용되는 Request DTO 클래스
 */
public record ReviewReplyPageableRequest(
	Long cursor,
	Long pageSize
) {

	@Builder
	public ReviewReplyPageableRequest {
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 50L;
	}
}
