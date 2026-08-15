package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.ExploreStandardResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailResponse;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.PaginationException;
import app.bottlenote.global.pagination.PaginationExceptionCode;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
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
  private final HmacCursorCodec cursorCodec;

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
  public PageResponse<ExploreStandardResponse> getStandardExplore(
      ExploreStandardRequest request, Long userId) {
    long resolvedSeed = resolveSeed(request, userId);
    ExploreStandardCriteria criteria = ExploreStandardCriteria.of(request, userId, resolvedSeed);
    PageResponse<List<AlcoholDetailItem>> page =
        alcoholQueryRepository.getStandardExplore(criteria);
    return PageResponse.of(new ExploreStandardResponse(page.content()), page.pagination());
  }

  /** RANDOM은 커서 extra의 seed를 재사용하고, 첫 페이지는 서버가 생성한다. */
  private long resolveSeed(ExploreStandardRequest request, Long userId) {
    if (request.sortType() != SearchSortType.RANDOM) {
      return 0L;
    }
    if (request.cursor() != null) {
      String seed =
          cursorCodec
              .verify(request.cursor(), ExploreStandardCriteria.of(request, userId, 0L).context())
              .extra()
              .get("seed");
      if (seed == null || seed.isBlank()) {
        throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
      }
      try {
        return Long.parseLong(seed);
      } catch (NumberFormatException exception) {
        throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
      }
    }
    return ThreadLocalRandom.current().nextLong();
  }
}
