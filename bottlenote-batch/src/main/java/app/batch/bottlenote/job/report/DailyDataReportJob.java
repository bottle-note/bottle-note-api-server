package app.batch.bottlenote.job.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 일일 데이터 리포트 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyDataReportJob {
	public static final String DAILY_DATA_REPORT_JOB_NAME = "dailyDataReportJob";

	private final JdbcTemplate jdbcTemplate;

	/**
	 * 일일 데이터 리포트 Job을 생성합니다.
	 */
	@Bean
	public Job dailyDataReportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		log.debug("일일 데이터 리포트 Job 초기화 시작");

		Step collectDataStep = getCollectDataStep(jobRepository, transactionManager);

		log.debug("일일 데이터 리포트 Job 초기화 완료");
		return new JobBuilder(DAILY_DATA_REPORT_JOB_NAME, jobRepository)
			.start(collectDataStep)
			.build();
	}

	/**
	 * 데이터를 수집하고 저장하는 스텝을 생성합니다.
	 */
	private Step getCollectDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("collectDailyDataStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				LocalDate targetDate = LocalDate.now().minusDays(1);
				log.info("전날 데이터 수집 시작: {}", targetDate);

				LocalDateTime startOfDay = targetDate.atStartOfDay();
				LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

				Long newUsersCount = countNewUsers(startOfDay, endOfDay);
				Long newReviewsCount = countNewReviews(startOfDay, endOfDay);
				Long newRepliesCount = countNewReplies(startOfDay, endOfDay);
				Long newLikesCount = countNewLikes(startOfDay, endOfDay);

				saveDailyDataReport(targetDate, newUsersCount, newReviewsCount, newRepliesCount, newLikesCount);

				log.info("일일 데이터 리포트 수집 완료: {} - 유저: {}, 리뷰: {}, 댓글: {}, 좋아요: {}",
					targetDate, newUsersCount, newReviewsCount, newRepliesCount, newLikesCount);

				return RepeatStatus.FINISHED;
			}, transactionManager)
			.build();
	}

	private Long countNewUsers(LocalDateTime start, LocalDateTime end) {
		String sql = "SELECT COUNT(*) FROM users WHERE create_at >= ? AND create_at < ?";
		return jdbcTemplate.queryForObject(sql, Long.class, start, end);
	}

	private Long countNewReviews(LocalDateTime start, LocalDateTime end) {
		String sql = "SELECT COUNT(*) FROM reviews WHERE create_at >= ? AND create_at < ?";
		return jdbcTemplate.queryForObject(sql, Long.class, start, end);
	}

	private Long countNewReplies(LocalDateTime start, LocalDateTime end) {
		String sql = "SELECT COUNT(*) FROM review_reply WHERE create_at >= ? AND create_at < ?";
		return jdbcTemplate.queryForObject(sql, Long.class, start, end);
	}

	private Long countNewLikes(LocalDateTime start, LocalDateTime end) {
		String sql = "SELECT COUNT(*) FROM likes WHERE create_at >= ? AND create_at < ?";
		return jdbcTemplate.queryForObject(sql, Long.class, start, end);
	}

	private void saveDailyDataReport(LocalDate reportDate, Long newUsersCount, Long newReviewsCount,
									  Long newRepliesCount, Long newLikesCount) {
		String checkSql = "SELECT COUNT(*) FROM daily_data_reports WHERE report_date = ?";
		Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, reportDate);

		if (count != null && count > 0) {
			log.info("이미 존재하는 리포트입니다. 업데이트를 진행합니다: {}", reportDate);
			String updateSql = """
				UPDATE daily_data_reports
				SET new_users_count = ?, new_reviews_count = ?, new_replies_count = ?, new_likes_count = ?,
				    last_modify_at = NOW()
				WHERE report_date = ?
				""";
			jdbcTemplate.update(updateSql, newUsersCount, newReviewsCount, newRepliesCount, newLikesCount, reportDate);
		} else {
			String insertSql = """
				INSERT INTO daily_data_reports (report_date, new_users_count, new_reviews_count, new_replies_count, new_likes_count, webhook_sent, create_at)
				VALUES (?, ?, ?, ?, ?, FALSE, NOW())
				""";
			jdbcTemplate.update(insertSql, reportDate, newUsersCount, newReviewsCount, newRepliesCount, newLikesCount);
		}
	}
}
