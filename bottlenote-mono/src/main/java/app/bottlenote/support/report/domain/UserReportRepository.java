package app.bottlenote.support.report.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReportRepository {

  UserReports save(UserReports userReports);

  List<UserReports> findByUserIdAndCreateAtAfter(Long user_id, LocalDateTime date);
}
