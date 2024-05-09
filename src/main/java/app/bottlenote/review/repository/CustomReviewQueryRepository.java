package app.bottlenote.review.repository;


import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;

import java.util.List;

public interface CustomReviewQueryRepository {

	List<ReviewsDetailInfo.ReviewInfo> findBestReviewsForAlcoholDetail(Long alcoholId, Long userId);

	List<ReviewsDetailInfo.ReviewInfo> findReviewsForAlcoholDetail(Long alcoholId, Long userId, List<Long> ids);
}
