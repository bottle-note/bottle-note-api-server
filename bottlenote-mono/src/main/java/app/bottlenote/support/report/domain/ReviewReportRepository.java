package app.bottlenote.support.report.domain;

import java.util.Optional;

public interface ReviewReportRepository {

  ReviewReport save(ReviewReport reviewReport);

  Optional<Object> findByUserIdAndReviewId(Long userId, Long reviewId);

  Optional<Long> countUniqueIpReportsByReviewId(Long reviewId);
}
