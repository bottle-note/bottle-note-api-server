package app.bottlenote.rating.service;

import app.bottlenote.core.alcohols.application.AlcoholFacade;
import app.bottlenote.core.users.application.UserFacade;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.shared.cursor.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingQueryService {
  private final RatingRepository ratingRepository;
  private final UserFacade userFacade;
  private final AlcoholFacade alcoholFacade;

  @Transactional(readOnly = true)
  public PageResponse<RatingListFetchResponse> fetchRatingList(
      RatingListFetchRequest request, Long userId) {
    var criteria = RatingListFetchCriteria.of(request, userId);
    return ratingRepository.fetchRatingList(criteria);
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
