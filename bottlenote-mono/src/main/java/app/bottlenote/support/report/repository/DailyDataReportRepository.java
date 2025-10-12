package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.DailyDataReport;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyDataReportRepository {

  DailyDataReport save(DailyDataReport dailyDataReport);

  Optional<DailyDataReport> findByReportDate(LocalDate reportDate);
}
