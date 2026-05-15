package app.bottlenote.curation.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.ReviewRepository;
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
public class CurationAlcoholGraphqlService {

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
  public Double rating(Alcohol alcohol) {
    Long alcoholId = alcoholIdOf(alcohol);
    if (alcoholId == null) {
      return 0.0;
    }
    return ratingRepository.findAverageRatingByAlcoholId(alcoholId);
  }

  @Transactional(readOnly = true)
  public Long totalRatingsCount(Alcohol alcohol) {
    Long alcoholId = alcoholIdOf(alcohol);
    if (alcoholId == null) {
      return 0L;
    }
    return ratingRepository.countByAlcoholId(alcoholId);
  }

  @Transactional(readOnly = true)
  public Long reviewCount(Alcohol alcohol) {
    Long alcoholId = alcoholIdOf(alcohol);
    if (alcoholId == null) {
      return 0L;
    }
    return reviewRepository.countByAlcoholIdAndActiveStatusAndStatus(
        alcoholId, ReviewActiveStatus.ACTIVE, ReviewDisplayStatus.PUBLIC);
  }

  @Transactional(readOnly = true)
  public Long totalPickCount(Alcohol alcohol) {
    Long alcoholId = alcoholIdOf(alcohol);
    if (alcoholId == null) {
      return 0L;
    }
    return picksRepository.countByAlcoholIdAndStatus(alcoholId, PicksStatus.PICK);
  }

  private Long alcoholIdOf(Alcohol alcohol) {
    if (alcohol == null) {
      return null;
    }
    return alcohol.getId();
  }
}
