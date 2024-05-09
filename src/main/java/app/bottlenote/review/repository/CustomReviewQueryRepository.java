package app.bottlenote.review.repository;


import app.bottlenote.alcohols.dto.response.AlcoholDetail;

import java.util.List;

public interface CustomReviewQueryRepository {

	List<AlcoholDetail.ReviewOfAlcoholDetail> findBestReviewsForAlcoholDetail(Long alcoholId, Long userId);

	List<AlcoholDetail.ReviewOfAlcoholDetail> findReviewsForAlcoholDetail(Long alcoholId, Long userId, List<Long> ids);
}
