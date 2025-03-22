package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailInfo;
import app.bottlenote.alcohols.facade.payload.FriendInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.user.service.FollowFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {
	private static final int MAX_FRIENDS_SIZE = 6;
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
	@Transactional(readOnly = true)
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
	@Transactional(readOnly = true)
	public AlcoholDetailResponse findAlcoholDetailById(Long alcoholId, Long userId) {
		AlcoholDetailInfo alcoholDetail = alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId);
		FriendsDetailInfo friendInfos = getFriendInfos(alcoholId, userId);
		return AlcoholDetailResponse.builder()
			.alcohols(alcoholDetail)
			.friendsInfo(friendInfos)
			.reviewInfo(reviewFacade.getReviewInfoList(alcoholId, userId))
			.build();
	}

	/**
	 * 유자가 팔로우 한 사람들 중 해당 술(위스키)를 마셔본 리스트 조회 api
	 *
	 * @param alcoholId
	 * @param userId
	 * @return FriendsDetailInfo
	 */
	protected FriendsDetailInfo getFriendInfos(Long alcoholId, Long userId) {
		PageRequest pageRequest = PageRequest.of(0, MAX_FRIENDS_SIZE);
		List<FriendInfo> friendInfos = followFacade.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
		return FriendsDetailInfo.of((long) friendInfos.size(), friendInfos);
	}
}
