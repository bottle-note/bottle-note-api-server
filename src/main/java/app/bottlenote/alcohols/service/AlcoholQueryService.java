package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.constant.AlcoholType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryResponse;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.alcohols.dto.response.detail.FriendsDetailInfo;
import app.bottlenote.alcohols.dto.response.detail.ReviewsDetailInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.repository.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
		FriendsDetailInfo friendsData = getMockFriendsData();

		// 리뷰 조회
		List<ReviewsDetailInfo.ReviewInfo> bestReviewInfos = reviewQueryRepository.findBestReviewsForAlcoholDetail(alcoholId, userId);
		List<Long> bestReviewIds = bestReviewInfos.stream().map(ReviewsDetailInfo.ReviewInfo::reviewId).toList();
		List<ReviewsDetailInfo.ReviewInfo> reviewInfos = reviewQueryRepository.findReviewsForAlcoholDetail(alcoholId, userId, bestReviewIds);
		Long reviewTotalCount = reviewQueryRepository.countByAlcoholId(alcoholId);

		ReviewsDetailInfo reviewsDetailInfo = ReviewsDetailInfo.builder()
			.totalReviewCount(reviewTotalCount)
			.bestReviewInfos(bestReviewInfos)
			.recentReviewInfos(reviewInfos)
			.build();

		return AlcoholDetail.of(alcoholDetailById, friendsData, reviewsDetailInfo);
	}

	/**
	 * 유저의 팔로잉 팔로워 기능이 구현 후 수정이 필요한 기능입니다.
	 * 현재는 Mock 데이터를 리턴합니다.
	 * //todo 유저의 팔로잉 팔로워 기능이 구현 후 수정이 필요한 기능입니다.
	 */
	private FriendsDetailInfo getMockFriendsData() {
		String freeRandomImageUrl = "https://picsum.photos/600/600";

		List<FriendsDetailInfo.FriendInfo> friendInfos = List.of(
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 1L, "늙은코끼리", 4.5),
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 2L, "나무사자", 1.5),
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 3L, "피자파인애플", 3.0),
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 4L, "멘토스", 0.5),
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 5L, "민트맛치토스", 5.0),
			new FriendsDetailInfo.FriendInfo(freeRandomImageUrl, 6L, "목데이터", 1.0)
		);
		return FriendsDetailInfo.of(6L, friendInfos);
	}

	@Cacheable(value = "LC-AlcoholCategory")
	@Transactional(readOnly = true)
	public List<CategoryResponse> getAlcoholCategory(AlcoholType type) {
		return alcoholQueryRepository.findAllCategories(type);
	}
}
