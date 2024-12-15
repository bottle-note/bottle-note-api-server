package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.user.service.FollowFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {

	private final AlcoholQueryRepository alcoholQueryRepository;
	private final ReviewFacade reviewFacade;
	private final FollowFacade followFacade;

	/**
	 * 술(위스키) 리스트 조회 api
	 *
	 * @param request 검색 파라미터
	 * @param userId  현재 사용자 id
	 * @return the page response
	 */
	public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchRequest request, Long userId) {
		AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, userId);
		return alcoholQueryRepository.searchAlcohols(criteria);
	}

	/**
	 * 술(위스키) 상세 조회 api
	 *
	 * @param alcoholId the alcohol id
	 * @param userId    the user id
	 * @return the list
	 */
	public AlcoholDetail findAlcoholDetailById(Long alcoholId, Long userId) {
		AlcoholDetailInfo alcoholDetail = alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId);
		return AlcoholDetail.builder()
			.alcohols(alcoholDetail)
			.friendsInfo(followFacade.getTastingFriendsInfoList(alcoholId, userId))
			.reviewInfo(reviewFacade.getReviewInfoList(alcoholId, userId))
			.build();
	}
}
