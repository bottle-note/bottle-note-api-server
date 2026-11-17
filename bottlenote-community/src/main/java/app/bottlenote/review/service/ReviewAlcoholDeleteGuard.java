package app.bottlenote.review.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_HAS_REVIEWS;

import app.bottlenote.alcohols.domain.AlcoholDeleteGuard;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 리뷰가 있는 주류의 삭제를 막는다. */
@Component
@Order(AlcoholDeleteGuard.REVIEW_ORDER)
@RequiredArgsConstructor
public class ReviewAlcoholDeleteGuard implements AlcoholDeleteGuard {
  private final ReviewRepository reviewRepository;

  @Override
  public void verifyDeletable(Long alcoholId) {
    if (reviewRepository.existsByAlcoholId(alcoholId)) {
      throw new AlcoholException(ALCOHOL_HAS_REVIEWS);
    }
  }
}
