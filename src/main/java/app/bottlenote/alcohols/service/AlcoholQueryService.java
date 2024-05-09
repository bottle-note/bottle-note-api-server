package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static app.bottlenote.alcohols.dto.response.AlcoholDetail.ReviewOfAlcoholDetail;

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
	public List<?> findAlcoholDetailById(Long alcoholId, Long userId) {

		// 위스키 상세 조회

		// 팔로워 수 조회

		// 리뷰 조회
		List<ReviewOfAlcoholDetail> bestReviewsForAlcoholDetail = reviewQueryRepository.findBestReviewsForAlcoholDetail(alcoholId, userId);
		List<ReviewOfAlcoholDetail> reviewsForAlcoholDetail = reviewQueryRepository.findReviewsForAlcoholDetail(alcoholId, userId);

		return List.of(bestReviewsForAlcoholDetail, reviewsForAlcoholDetail);
	}
}
