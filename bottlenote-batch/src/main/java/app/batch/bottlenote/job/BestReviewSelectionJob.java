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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

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
	@Value("${command}")
	private String command;


	@Bean
	public Job bestReviewSelectedJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new JobBuilder("bestReviewSelectedJob", jobRepository)
			.start(reviewStep(jobRepository, transactionManager))
			.build();
	}

	@Bean
	public Step reviewStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("bestReviewSelectedStep", jobRepository)
			.<BestReviewPayload, BestReviewPayload>chunk(CHUNK_SIZE, transactionManager)
			.reader(new BestReviewReader(jdbcTemplate, decodeQuery()))
			.processor(item -> item)
			.writer(this::updateBestReviews)
			.build();
	}

	private String decodeQuery() {
		log.info("command: {}", command);
		return """
			SELECT id, alcohol_id, popularityScore, likeCount, unlikeCount, replyCount, imageCount,reviewCount
			FROM (SELECT r.id,
			             r.alcohol_id,
			             (SUM(IF(l.status = 'LIKE', 1, 0)) * 0.6 + COUNT(rr.id) * 0.2 + IF(COUNT(ri.id) > 0, 0.2, 0) -
			              SUM(IF(l.status = 'DISLIKE', 1, 0)) * 0.1)           AS popularityScore,
			             SUM(IF(l.status = 'LIKE', 1, 0))                      AS likeCount,
			             SUM(IF(l.status = 'DISLIKE', 1, 0))                   AS unlikeCount,
			             COUNT(rr.id)                                          AS replyCount,
			             COUNT(ri.id)                                          AS imageCount,
			             ROW_NUMBER() OVER (PARTITION BY r.alcohol_id ORDER BY
			                 (SUM(IF(l.status = 'LIKE', 1, 0)) * 0.6 + COUNT(rr.id) * 0.2 + IF(COUNT(ri.id) > 0, 0.2, 0) -
			                  SUM(IF(l.status = 'DISLIKE', 1, 0)) * 0.1) DESC) AS row_num,
			             COUNT(r.id) OVER (PARTITION BY r.alcohol_id)             AS reviewCount
			      FROM review r
			               LEFT JOIN review_reply rr ON r.id = rr.review_id
			               LEFT JOIN review_image ri ON r.id = ri.review_id
			               LEFT JOIN likes l ON r.id = l.review_id
			      GROUP BY r.id, r.alcohol_id) AS ranked_reviews
			WHERE (ranked_reviews.reviewCount < 10 AND ranked_reviews.row_num <= 1)
			   OR (ranked_reviews.reviewCount >= 10 AND ranked_reviews.reviewCount < 20 AND ranked_reviews.row_num <= 2)
			   OR (ranked_reviews.reviewCount >= 20 AND ranked_reviews.row_num <= 3)
			ORDER BY alcohol_id, popularityScore DESC;
			""";
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
