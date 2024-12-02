package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
}
