package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.DailyDataReport;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDailyDataReportDao extends JpaRepository<DailyDataReport, Long> {

  Optional<DailyDataReport> findByReportDate(LocalDate reportDate);
}
