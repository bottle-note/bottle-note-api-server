package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse.TastingTagInfo;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.repository.CustomAlcoholQueryRepository.AdminAlcoholDetailProjection;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
    AlcoholDetailItem alcoholDetail =
        alcoholQueryRepository.findAlcoholDetailById(alcoholId, userId);

    // 조회 기록 저장 (게스트 사용자 제외)
    if (userId > 0 && alcoholDetail != null) viewHistoryService.recordView(userId, alcoholDetail);

    FriendsDetailResponse friendInfos = getFriendInfos(alcoholId, userId);

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
   * @return FriendsDetailResponse
   */
  protected FriendsDetailResponse getFriendInfos(Long alcoholId, Long userId) {
    PageRequest pageRequest = PageRequest.of(0, MAX_FRIENDS_SIZE);
    List<FriendItem> friendItems =
        followFacade.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
    return FriendsDetailResponse.of((long) friendItems.size(), friendItems);
  }

  @Transactional(readOnly = true)
  public Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size) {
    return alcoholQueryRepository.getStandardExplore(userId, keywords, cursor, size);
  }

  @Transactional(readOnly = true)
  public GlobalResponse searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    return GlobalResponse.fromPage(alcoholQueryRepository.searchAdminAlcohols(request));
  }

  @Transactional(readOnly = true)
  public List<Pair<String, String>> findAllCategoryPairs() {
    return alcoholQueryRepository.findAllCategoryPairs();
  }

  @Transactional(readOnly = true)
  public AdminAlcoholDetailResponse findAdminAlcoholDetailById(Long alcoholId) {
    AdminAlcoholDetailProjection projection =
        alcoholQueryRepository
            .findAdminAlcoholDetailById(alcoholId)
            .orElseThrow(() -> new AlcoholException(AlcoholExceptionCode.ALCOHOL_NOT_FOUND));

    Alcohol alcohol =
        alcoholQueryRepository
            .findById(alcoholId)
            .orElseThrow(() -> new AlcoholException(AlcoholExceptionCode.ALCOHOL_NOT_FOUND));

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
