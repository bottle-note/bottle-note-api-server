package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {

	private final AlcoholQueryRepository alcoholQueryRepository;
	private final ReviewQueryRepository reviewQueryRepository;

	/**
	 * 술(위스키) 리스트 조회 api
	 *
	 * @param request 검색 파라미터
	 * @param userId  현재 사용자 id
	 * @return the page response
	 */
	public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchRequest request, Long userId) {

		AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, userId);

		log.info("searchAlcohols criteria: {}", criteria);

		return alcoholQueryRepository.searchAlcohols(criteria);
	}


	/**
	 * 술(위스키) 상세 조회 api
	 *
	 * @param alcoholId the alcohol id
	 * @param userId    the user id
	 * @return the list
	 */
	@Transactional(readOnly = true)
	public AlcoholDetail findAlcoholDetailById(Long alcoholId, Long userId) {

		// 위스키 상세 조회
		AlcoholDetailInfo alcoholDetailById = alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId);

		// 팔로워 수 조회


		// 리뷰 조회
		List<ReviewsDetailInfo.ReviewInfo> bestReviewInfos = reviewQueryRepository.findBestReviewsForAlcoholDetail(alcoholId, userId);
		List<Long> bestReviewIds = bestReviewInfos.stream().map(ReviewsDetailInfo.ReviewInfo::reviewId).toList();
		List<ReviewsDetailInfo.ReviewInfo> reviewInfos = reviewQueryRepository.findReviewsForAlcoholDetail(alcoholId, userId, bestReviewIds);
		ReviewsDetailInfo reviewsDetailInfo = ReviewsDetailInfo.builder()
			.bestReviewInfos(bestReviewInfos)
			.recentReviewInfos(reviewInfos)
			.build();

		return AlcoholDetail.of(alcoholDetailById, null, reviewsDetailInfo);
	}
}
