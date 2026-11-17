package app.bottlenote.curation.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.response.AlcoholPicksCountResponse;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.response.AlcoholRatingStatsResponse;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.response.AlcoholReviewCountResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphQLCurationAlcoholService {

  private final AlcoholQueryRepository alcoholQueryRepository;
  private final RatingRepository ratingRepository;
  private final ReviewRepository reviewRepository;
  private final PicksRepository picksRepository;

  @Transactional(readOnly = true)
  public List<Alcohol> findAlcohols(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }

    List<Long> alcoholIds = ids.stream().filter(Objects::nonNull).distinct().toList();
    if (alcoholIds.isEmpty()) {
      return List.of();
    }

    Map<Long, Alcohol> alcoholsById =
        alcoholQueryRepository.findAllByIdIn(alcoholIds).stream()
            .filter(alcohol -> alcohol.getDeletedAt() == null)
            .collect(Collectors.toMap(Alcohol::getId, Function.identity(), (left, right) -> left));

    return alcoholIds.stream().map(alcoholsById::get).filter(Objects::nonNull).toList();
  }

  @Transactional(readOnly = true)
  public String regionName(Alcohol alcohol) {
    if (alcohol == null || alcohol.getRegion() == null) {
      return null;
    }
    return alcohol.getRegion().getKorName();
  }

  @Transactional(readOnly = true)
  public Map<Long, AlcoholRatingStatsResponse> ratingStats(List<Alcohol> alcohols) {
    List<Long> alcoholIds = alcoholIdsOf(alcohols);
    if (alcoholIds.isEmpty()) {
      return Map.of();
    }
    return ratingRepository.findStatsByAlcoholIds(alcoholIds).stream()
        .collect(Collectors.toMap(AlcoholRatingStatsResponse::alcoholId, Function.identity()));
  }

  @Transactional(readOnly = true)
  public Map<Long, Long> reviewCounts(List<Alcohol> alcohols) {
    List<Long> alcoholIds = alcoholIdsOf(alcohols);
    if (alcoholIds.isEmpty()) {
      return Map.of();
    }
    return reviewRepository
        .countByAlcoholIdsAndActiveStatusAndStatus(
            alcoholIds, ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PUBLIC)
        .stream()
        .collect(
            Collectors.toMap(
                AlcoholReviewCountResponse::alcoholId, AlcoholReviewCountResponse::reviewCount));
  }

  @Transactional(readOnly = true)
  public Map<Long, Long> pickCounts(List<Alcohol> alcohols) {
    List<Long> alcoholIds = alcoholIdsOf(alcohols);
    if (alcoholIds.isEmpty()) {
      return Map.of();
    }
    return picksRepository.countByAlcoholIdsAndStatus(alcoholIds, PicksStatus.PICK).stream()
        .collect(
            Collectors.toMap(
                AlcoholPicksCountResponse::alcoholId, AlcoholPicksCountResponse::totalPickCount));
  }

  private List<Long> alcoholIdsOf(List<Alcohol> alcohols) {
    return alcohols == null
        ? List.of()
        : alcohols.stream().map(this::alcoholIdOf).filter(Objects::nonNull).distinct().toList();
  }

  private Long alcoholIdOf(Alcohol alcohol) {
    if (alcohol == null) {
      return null;
    }
    return alcohol.getId();
  }
}
