package app.bottlenote.review.service;

import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewExploreService {
  private final ReviewRepository reviewRepository;

  @Transactional(readOnly = true)
  public Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size) {
    return reviewRepository.getStandardExplore(userId, keywords, cursor, size);
  }
}
