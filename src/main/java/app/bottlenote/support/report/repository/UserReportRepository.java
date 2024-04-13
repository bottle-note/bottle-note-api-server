package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.UserReports;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends CrudRepository<UserReports, Long> {
}
