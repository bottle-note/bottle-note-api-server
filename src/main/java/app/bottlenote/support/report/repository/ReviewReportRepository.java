package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

	Optional<Object> findByUserIdAndReviewId(Long currentUserId, Long reportReviewId);

	@Query("""
		SELECT COUNT(DISTINCT r.ipAddress)
		FROM review_report r
		WHERE r.reviewId = :reviewId
		""")
	Optional<Long> countUniqueIpReportsByReviewId(@Param("reviewId") Long reviewId);
}
