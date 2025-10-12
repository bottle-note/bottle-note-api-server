package app.bottlenote.support.report.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.support.report.domain.DailyDataReport;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class JpaDailyDataReportRepository implements DailyDataReportRepository {

  private final JpaDailyDataReportDao jpaDailyDataReportDao;

  @Override
  public DailyDataReport save(DailyDataReport dailyDataReport) {
    return jpaDailyDataReportDao.save(dailyDataReport);
  }

  @Override
  public Optional<DailyDataReport> findByReportDate(LocalDate reportDate) {
    return jpaDailyDataReportDao.findByReportDate(reportDate);
  }
}
