package app.batch.bottlenote.job.ranking;

import app.batch.bottlenote.BatchQuartzJob;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileCopyUtils;

/**
 * 베스트 리뷰 선정 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BestReviewSelectionJobConfig {
	public static final String BEST_REVIEW_JOB_NAME = "bestReviewSelectedJob";
	private static final int CHUNK_SIZE = 100;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 베스트 리뷰 선정 Job을 생성합니다.
	 */
	@Bean
	public Job bestReviewSelectedJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		log.debug("베스트 리뷰 선정 Job 초기화 시작");
		Step resetStep = getBestReviewResetStep(jobRepository, transactionManager);

		Step selectionStep = getBestReviewSelectedStep(jobRepository, transactionManager);
		if (selectionStep == null) {
			log.error("[CRITICAL] 베스트 리뷰 선정 Job 초기화 실패: 필요한 스텝을 생성할 수 없습니다.");
			return null;
		}

		log.debug("베스트 리뷰 선정 Job 초기화 완료");
		return new JobBuilder(BEST_REVIEW_JOB_NAME, jobRepository)
				.start(resetStep)
				.next(selectionStep)
				.build();
	}

	/**
	 * 모든 리뷰의 베스트 표시를 초기화하는 스텝을 생성합니다.
	 */
	private Step getBestReviewResetStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("resetBestReviewStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					String sql = "UPDATE reviews SET is_best = FALSE";
					int count = jdbcTemplate.update(sql);
					log.debug("베스트 리뷰 초기화 수행: {} 개 리뷰 처리됨", count);
					return RepeatStatus.FINISHED;
				}, transactionManager)
				.build();
	}

	/**
	 * 새로운 베스트 리뷰를 선정하는 스텝을 생성합니다.
	 */
	public Step getBestReviewSelectedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		String query = getQueryByResource();
		if (query == null) {
			log.error("[CRITICAL] 베스트 리뷰 선정 리소스 파일을 찾을 수 없어 작업을 중단합니다.");
			return null;
		}

		return new StepBuilder("bestReviewSelectedStep", jobRepository)
				.<BestReviewPayload, BestReviewPayload>chunk(CHUNK_SIZE, transactionManager)
				.reader(new BestReviewReader(jdbcTemplate, query))
				.processor(item -> item)
				.writer(this::updateBestReviews)
				.build();
	}

	/**
	 * 베스트 리뷰 선정을 위한 SQL 쿼리를 리소스 파일에서 로드합니다.
	 */
	private String getQueryByResource() {
		try {
			Resource resource = new ClassPathResource("storage/mysql/sql/best-review-selected.sql");
			String query = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
			log.debug("베스트 리뷰 쿼리 로드 완료: {}", resource.getDescription());
			return query;
		} catch (IOException e) {
			log.error("[CRITICAL] 베스트 리뷰 SQL 파일을 찾을 수 없습니다: best-review-selected.sql", e);
			return null;
		}
	}

	/**
	 * 선정된 베스트 리뷰들의 상태를 업데이트합니다.
	 */
	private void updateBestReviews(Chunk<? extends BestReviewPayload> chunk) {
		if (chunk.isEmpty()) return;

		String sql = "UPDATE reviews SET is_best = true WHERE id IN (" +
				String.join(",", Collections.nCopies(chunk.size(), "?")) + ")";

		Object[] params = chunk.getItems().stream()
				.map(BestReviewPayload::id)
				.toArray();

		int updated = jdbcTemplate.update(sql, params);
		log.debug("베스트 리뷰 업데이트 진행: {} 개 리뷰 업데이트됨", updated);
	}

	/**
	 * 베스트 리뷰 데이터를 읽는 클래스입니다.
	 */
	private static class BestReviewReader implements ItemReader<BestReviewPayload> {
		private final JdbcTemplate jdbcTemplate;
		private final String query;
		private List<BestReviewPayload> results;
		private int currentIndex;

		public BestReviewReader(JdbcTemplate jdbcTemplate, String query) {
			this.jdbcTemplate = jdbcTemplate;
			this.query = query;
		}

		@Override
		public BestReviewPayload read() {
			if (results == null) {
				results = jdbcTemplate.query(query, new BestReviewPayload.BestReviewMapper());
				currentIndex = 0;
			}

			if (currentIndex >= results.size()) {
				return null;
			}

			return results.get(currentIndex++);
		}
	}

	@Component
	public static class BestReviewQuartzJob extends BatchQuartzJob {
		public BestReviewQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
			super(jobLauncher, jobRegistry, BEST_REVIEW_JOB_NAME, "bestReviewSelectedJob");
		}
	}

	@Builder
	public record BestReviewPayload(Long id, Long alcoholId) {
		public static class BestReviewMapper implements RowMapper<BestReviewPayload> {
			@Override
			public BestReviewPayload mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new BestReviewPayload(rs.getLong("id"), rs.getLong("alcohol_id"));
			}
		}
	}
}
