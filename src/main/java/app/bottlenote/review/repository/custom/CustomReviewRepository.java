package app.bottlenote.review.repository.custom;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.vo.ReviewInfo;

public interface CustomReviewRepository {

	/***
	 * 특정 리뷰에 대한 리뷰 상세 정보(댓글 포함)를 조회합니다.
	 *
	 * @param reviewId 조회 대상 리뷰 ID
	 * @param userId 조회하는 사용자 ID
	 * @return 리뷰 상세 정보
	 */
	ReviewInfo getReview(Long reviewId, Long userId);


	/**
	 * 특정 술에 대한 전체 리뷰 목록을 조회합니다
	 *
	 * @param alcoholId             조회 대상 알코올 ID
	 * @param reviewPageableRequest 정렬기준, 페이징 처리 기준
	 * @param userId                조회하는 사용자 ID
	 * @return 특정 술에 대한 전체 리뷰 목록
	 */

	PageResponse<ReviewListResponse> getReviews(Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

	/**
	 * 특정 술에 대해 로그인한 사용자가 작성한 리뷰 목록을 조회합니다.
	 *
	 * @param alcoholId             조회 대상 알코올 ID
	 * @param reviewPageableRequest 정렬기준, 페이징 처리 기준
	 * @param userId                조회하는 사용자 ID
	 * @return 특정 술에 대한 내가 작성한 리뷰 목록
	 */

	PageResponse<ReviewListResponse> getReviewsByMe(Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);
}
