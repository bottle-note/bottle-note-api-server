package app.bottlenote.support.report.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.support.report.domain.UserReportRepository;
import app.bottlenote.support.report.domain.UserReports;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@JpaRepositoryImpl
public interface JpaUserReportRepository
    extends UserReportRepository, CrudRepository<UserReports, Long> {

  @Override
  @Query(
      "SELECT r FROM user_report r JOIN FETCH r.user JOIN FETCH r.reportUser WHERE r.user.id = :user_id AND r.createAt >= :date")
  List<UserReports> findByUserIdAndCreateAtAfter(Long user_id, LocalDateTime date);
}
