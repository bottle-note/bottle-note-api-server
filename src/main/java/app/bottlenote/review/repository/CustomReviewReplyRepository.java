package app.bottlenote.review.repository;

import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;

public interface CustomReviewReplyRepository {
	/**
	 * 최상위 댓글 목록을 조회합니다.
	 *
	 * @param reviewId 리뷰 ID
	 * @param cursor   페이지 번호
	 * @param pageSize 페이지 사이즈
	 * @return 최상위 댓글 목록
	 */
	RootReviewReplyResponse getReviewRootReplies(Long reviewId, Long cursor, Long pageSize);


	/**
	 * 특정 최사위 댓글의 대댓글 목록을 조회합니다.
	 *
	 * @param reviewId 리뷰 ID
	 * @param replyId  부모 댓글 ID
	 * @param cursor   페이지 번호
	 * @param pageSize 페이지 사이즈
	 * @return 댓글 목록
	 */
	SubReviewReplyResponse getSubReviewReplies(Long reviewId, Long replyId, Long cursor, Long pageSize);

}
