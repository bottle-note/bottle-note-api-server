package app.bottlenote.support.report.repository;

import app.bottlenote.support.report.domain.UserReports;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserReportRepository extends CrudRepository<UserReports, Long> {

	@Query("SELECT r FROM user_report r JOIN FETCH r.user JOIN FETCH r.reportUser WHERE r.user.id = :user_id AND r.createAt >= :date")
	List<UserReports> findByUserIdAndCreateAtAfter(Long user_id, LocalDateTime date);
}
