package app.batch.bottlenote.job;

import app.batch.bottlenote.data.payload.BestReviewPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.List;

/**
 * 베스트 리뷰 선정 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BestReviewSelectionJob {
	private static final int CHUNK_SIZE = 10;
	private final JdbcTemplate jdbcTemplate;

	@Bean
	public Job bestReviewSelectedJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		Step step = reviewStep(jobRepository, transactionManager);
		if (step == null) {
			return null;
		}
		return new JobBuilder("bestReviewSelectedJob", jobRepository)
			.start(step)
			.build();
	}

	@Bean
	public Step reviewStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		String query = getQueryByResource();
		if (query == null) {
			log.info("베스트 리뷰 선정 리소스 파일을 찾을 수 없습니다 .");
			return null;
		}

		return new StepBuilder("bestReviewSelectedStep", jobRepository)
			.<BestReviewPayload, BestReviewPayload>chunk(CHUNK_SIZE, transactionManager)
			.reader(new BestReviewReader(jdbcTemplate, query))
			.processor(item -> item)
			.writer(this::updateBestReviews)
			.build();
	}

	private String getQueryByResource() {
		try {
			// resources 디렉토리 하위의 파일 경로로 접근
			Resource resource = new ClassPathResource("best-review-selected.sql");
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		} catch (IOException e) {
			log.error("cant find best-review-selected.sql files", e);
			return null;
		}
	}

	private void updateBestReviews(Chunk<? extends BestReviewPayload> chunk) {
		String updateSql = "UPDATE review SET is_best = true WHERE id = ?";
		for (BestReviewPayload item : chunk) {
			jdbcTemplate.update(updateSql, item.id());
			log.info("Best review updated: review id: {}", item.id());
		}
	}

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

			BestReviewPayload nextItem = null;
			if (currentIndex < results.size()) {
				nextItem = results.get(currentIndex);
				currentIndex++;
			}

			if (currentIndex >= results.size()) {
				results = null;
			}

			return nextItem;
		}
	}
}
