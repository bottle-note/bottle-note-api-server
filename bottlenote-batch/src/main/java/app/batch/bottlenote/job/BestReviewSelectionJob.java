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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 베스트 리뷰 선정 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BestReviewSelectionJob {
    public static final String BEST_REVIEW_JOB_NAME = "bestReviewSelectedJob";
    private static final int CHUNK_SIZE = 10;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Job bestReviewSelectedJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Step resetStep = getBestReviewResetStep(jobRepository, transactionManager);

        Step selectionStep = getBestReviewSelectedStep(jobRepository, transactionManager);
        if (selectionStep == null) {
            return null;
        }

        return new JobBuilder(BEST_REVIEW_JOB_NAME, jobRepository)
                .start(resetStep)
                .next(selectionStep)
                .build();
    }

    private Step getBestReviewResetStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("resetBestReviewStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int count = jdbcTemplate.update("update review set is_best = false");
                    log.info("Best review reset: count: {}", count);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    public Step getBestReviewSelectedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
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
            log.info("베스트 리뷰 쿼리 로드 완료");
            return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
        } catch (IOException e) {
            log.error("cant find best-review-selected.sql files", e);
            return null;
        }
    }

    private void updateBestReviews(Chunk<? extends BestReviewPayload> chunk) {
        if (chunk.isEmpty()) return;

        // 배치 업데이트를 위한 SQL 준비
        String sql = "UPDATE review SET is_best = true WHERE id IN (" +
                String.join(",", Collections.nCopies(chunk.size(), "?")) + ")";

        // 파라미터 배열 준비
        Object[] params = chunk.getItems().stream()
                .map(BestReviewPayload::id)
                .toArray();

        int updated = jdbcTemplate.update(sql, params);
        log.info("Batch updated {} best reviews", updated);
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
