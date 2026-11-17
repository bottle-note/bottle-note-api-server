package app.bottlenote.support.report.domain;

import java.util.List;
import java.util.Optional;

public interface ReviewReportRepository {

  ReviewReport save(ReviewReport reviewReport);

  Optional<Object> findByUserIdAndReviewId(Long userId, Long reviewId);

  Optional<Long> countUniqueIpReportsByReviewId(Long reviewId);

  List<ReviewReport> findAll();

  Optional<ReviewReport> findById(Long id);
}
