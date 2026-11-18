package app.bottlenote.rating.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_HAS_RATINGS;

import app.bottlenote.alcohols.domain.AlcoholDeleteGuard;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.rating.domain.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 별점이 있는 주류의 삭제를 막는다. */
@Component
@Order(AlcoholDeleteGuard.RATING_ORDER)
@RequiredArgsConstructor
public class RatingAlcoholDeleteGuard implements AlcoholDeleteGuard {
  private final RatingRepository ratingRepository;

  @Override
  public void verifyDeletable(Long alcoholId) {
    if (ratingRepository.existsByAlcoholId(alcoholId)) {
      throw new AlcoholException(ALCOHOL_HAS_RATINGS);
    }
  }
}
