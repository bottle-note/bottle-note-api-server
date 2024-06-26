package app.bottlenote.review.repository.custom;


import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;

import java.util.List;

public interface CustomReviewQueryRepository {
	/**
	 * 특정 술의 베스트 리뷰를 조회합니다.
	 *
	 * @param alcoholId 조회 대상 알코올 ID
	 * @param userId    조회하는 사용자 ID
	 * @return 베스트 리뷰 목록
	 */
	List<ReviewsDetailInfo.ReviewInfo> fetchTopReviewByAlcohol(Long alcoholId, Long userId);

	/**
	 * 특정 술에 대한 최신 리뷰를 조회합니다. (최대 4개, 베스트 리뷰 제외)
	 *
	 * @param alcoholId 조회 대상 알코올 ID
	 * @param userId    조회하는 사용자 ID
	 * @param ids       제외할 리뷰 ID 목록
	 * @return 최신 리뷰 목록
	 */
	List<ReviewsDetailInfo.ReviewInfo> fetchLatestReviewsByAlcoholExcludingIds(Long alcoholId, Long userId, List<Long> ids);

	/**
	 * 특정 술에 대한 리뷰 개수를 조회합니다.
	 *
	 * @param alcoholId the alcohol id
	 * @return the long
	 */
	Long countByAlcoholId(Long alcoholId);

	/**
	 * 알콜 상세 조회 시 사용자의 리뷰 목록을 조회합니다.
	 *
	 * @param userId 조회하는 사용자 ID
	 * @return 사용자의 리뷰 목록
	 */
	ReviewsDetailInfo fetchUserReviewsForAlcoholDetail(Long alcoholId, Long userId);
}
