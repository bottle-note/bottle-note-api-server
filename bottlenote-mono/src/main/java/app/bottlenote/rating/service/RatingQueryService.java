package app.bottlenote.rating.service;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.user.facade.UserFacade;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingQueryService {
  private final RatingRepository ratingRepository;
  private final UserFacade userFacade;
  private final AlcoholFacade alcoholFacade;
  private final HmacCursorCodec cursorCodec;

  @Transactional(readOnly = true)
  public PageResponse<RatingListFetchResponse> fetchRatingList(
      RatingListFetchRequest request, Long userId) {
    long resolvedSeed = resolveSeed(request, userId);
    var criteria = RatingListFetchCriteria.of(request, userId, resolvedSeed);
    return ratingRepository.fetchRatingList(criteria);
  }

  /** RANDOM은 커서 extra의 seed를 재사용하고, 첫 페이지는 서버가 생성한다. */
  private long resolveSeed(RatingListFetchRequest request, Long userId) {
    if (request.sortType() != SearchSortType.RANDOM) {
      return 0L;
    }
    if (request.cursor() != null) {
      var claims =
          cursorCodec.verify(
              request.cursor(), RatingListFetchCriteria.of(request, userId, 0L).context());
      return CursorKeys.requireExtraLong(claims, "seed");
    }
    return ThreadLocalRandom.current().nextLong();
  }

  @Transactional(readOnly = true)
  public UserRatingResponse fetchUserRating(Long alcoholId, Long userId) {
    userFacade.isValidUserId(userId);
    alcoholFacade.isValidAlcoholId(alcoholId);

    return ratingRepository
        .fetchUserRating(alcoholId, userId)
        .orElse(UserRatingResponse.empty(alcoholId, userId));
  }
}
