package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse.TastingTagInfo;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.CategoryPairItem;
import app.bottlenote.alcohols.dto.response.ExploreStandardResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.CustomAlcoholQueryRepository.AdminAlcoholDetailProjection;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholQueryService {
  private static final int MAX_FRIENDS_SIZE = 6;
  private final AlcoholQueryRepository alcoholQueryRepository;
  private final AlcoholViewHistoryService viewHistoryService;
  private final ReviewFacade reviewFacade;
  private final FollowFacade followFacade;
  private final AlcoholReferenceService alcoholReferenceService;

  /**
   * 술(위스키) 리스트 조회 api
   *
   * @param request 검색 파라미터
   * @param userId 현재 사용자 id
   * @return the page response
   */
  @Transactional(readOnly = true)
  public PageResponse<AlcoholSearchResponse> searchAlcohols(
      AlcoholSearchRequest request, Long userId) {
    AlcoholSearchCriteria criteria = AlcoholSearchCriteria.of(request, userId);
    return alcoholQueryRepository.searchAlcohols(criteria);
  }

  /**
   * 술(위스키) 상세 조회 api
   *
   * @param alcoholId the alcohol id
   * @param userId the user id
   * @return the list
   */
  @Transactional(readOnly = true)
  public AlcoholDetailResponse findAlcoholDetailById(Long alcoholId, Long userId) {
    AlcoholDetailItem alcoholDetailItem =
        Optional.ofNullable(alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId))
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    // 조회 기록 저장 (게스트 사용자 제외)
    if (userId > 0) viewHistoryService.recordView(userId, alcoholDetailItem);

    FriendsDetailResponse friendInfos = getFriendInfos(alcoholId, userId);

    return AlcoholDetailResponse.builder()
        .alcohols(alcoholDetailItem)
        .friendsInfo(friendInfos)
        .reviewInfo(reviewFacade.getReviewInfoList(alcoholId, userId))
        .build();
  }

  /**
   * 유자가 팔로우 한 사람들 중 해당 술(위스키)를 마셔본 리스트 조회 api
   *
   * @param alcoholId
   * @param userId
   * @return FriendsDetailResponse
   */
  protected FriendsDetailResponse getFriendInfos(Long alcoholId, Long userId) {
    PageRequest pageRequest = PageRequest.of(0, MAX_FRIENDS_SIZE);
    List<FriendItem> friendItems =
        followFacade.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
    return FriendsDetailResponse.of((long) friendItems.size(), friendItems);
  }

  @Transactional(readOnly = true)
  public ExploreStandardResponse getStandardExplore(ExploreStandardRequest request, Long userId) {
    long resolvedSeed = resolveSeed(request);
    ExploreStandardCriteria criteria = ExploreStandardCriteria.of(request, userId, resolvedSeed);
    CursorResponse<AlcoholDetailItem> page = alcoholQueryRepository.getStandardExplore(criteria);
    return new ExploreStandardResponse(resolvedSeed, page);
  }

  /** RANDOM 정렬 시 요청 seed 를 그대로 쓰고, 없으면 서버에서 생성한다. 비-RANDOM 정렬은 seed 가 쿼리에 영향을 주지 않으므로 0 으로 고정한다. */
  private long resolveSeed(ExploreStandardRequest request) {
    if (request.sortType() != SearchSortType.RANDOM) {
      return 0L;
    }
    return request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
  }

  @Transactional(readOnly = true)
  public GlobalResponse searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    return GlobalResponse.fromPage(alcoholQueryRepository.searchAdminAlcohols(request));
  }

  /** 카테고리 레퍼런스를 categoryGroup 기준으로 묶어 반환한다. enum 선언 순서로 키가 고정되며, 데이터가 없는 그룹도 빈 리스트로 포함된다. */
  @Transactional(readOnly = true)
  public Map<AlcoholCategoryGroup, List<CategoryPairItem>> findAllCategoryReferenceMap() {
    Map<AlcoholCategoryGroup, List<CategoryPairItem>> grouped =
        new EnumMap<>(AlcoholCategoryGroup.class);
    for (AlcoholCategoryGroup group : AlcoholCategoryGroup.values()) {
      grouped.put(group, new ArrayList<>());
    }

    for (CategoryItem item : alcoholQueryRepository.findAllCategoryItems()) {
      AlcoholCategoryGroup group = item.categoryGroup();
      if (group == null) continue;
      grouped.get(group).add(new CategoryPairItem(item.korCategory(), item.engCategory()));
    }

    return grouped;
  }

  @Transactional(readOnly = true)
  public AdminAlcoholDetailResponse findAdminAlcoholDetailById(Long alcoholId) {
    AdminAlcoholDetailProjection projection =
        alcoholQueryRepository
            .findAdminAlcoholDetailById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    Alcohol alcohol =
        alcoholQueryRepository
            .findById(alcoholId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

    List<TastingTagInfo> tastingTags =
        alcohol.getAlcoholsTastingTags().stream()
            .map(
                att ->
                    new TastingTagInfo(
                        att.getTastingTag().getId(),
                        att.getTastingTag().getKorName(),
                        att.getTastingTag().getEngName()))
            .toList();

    return new AdminAlcoholDetailResponse(
        projection.alcoholId(),
        projection.korName(),
        projection.engName(),
        projection.imageUrl(),
        projection.type(),
        projection.korCategory(),
        projection.engCategory(),
        projection.categoryGroup(),
        projection.abv(),
        projection.age(),
        projection.cask(),
        projection.volume(),
        projection.description(),
        projection.regionId(),
        projection.korRegion(),
        projection.engRegion(),
        projection.distilleryId(),
        projection.korDistillery(),
        projection.engDistillery(),
        tastingTags,
        projection.avgRating(),
        projection.totalRatingsCount(),
        projection.reviewCount(),
        projection.pickCount(),
        projection.createdAt(),
        projection.modifiedAt());
  }
}
