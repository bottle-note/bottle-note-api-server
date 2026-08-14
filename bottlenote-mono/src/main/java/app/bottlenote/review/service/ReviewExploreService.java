package app.bottlenote.review.service;

import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import app.bottlenote.review.dto.response.ReviewExploreListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewExploreService {
  private final ReviewRepository reviewRepository;

  @Transactional(readOnly = true)
  public PageResponse<ReviewExploreListResponse> getStandardExplore(
      ReviewExploreRequest request, Long userId) {
    return reviewRepository.getStandardExplore(
        userId, request.keywords(), request.cursor(), request.size());
  }
}
