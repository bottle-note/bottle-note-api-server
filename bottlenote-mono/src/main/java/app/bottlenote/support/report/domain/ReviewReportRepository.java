package app.bottlenote.support.report.domain;

import java.util.Optional;

public interface ReviewReportRepository {

  Optional<Object> findByUserIdAndReviewId(Long userId, Long reviewId);

  Optional<Long> countUniqueIpReportsByReviewId(Long reviewId);
}
