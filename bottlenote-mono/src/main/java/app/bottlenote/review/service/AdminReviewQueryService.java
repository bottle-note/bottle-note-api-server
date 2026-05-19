package app.bottlenote.review.service;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.AdminReviewSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewQueryService {

  private final ReviewRepository reviewRepository;

  @Transactional(readOnly = true)
  public GlobalResponse searchReviews(AdminReviewSearchRequest request) {
    return GlobalResponse.fromPage(reviewRepository.searchAdminReviews(request));
  }
}
