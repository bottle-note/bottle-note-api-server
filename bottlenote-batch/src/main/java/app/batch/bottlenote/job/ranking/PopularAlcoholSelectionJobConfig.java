package app.batch.bottlenote.job.ranking;

import app.batch.bottlenote.BatchQuartzJob;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * 인기 주류 선정 배치 Job
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PopularAlcoholSelectionJobConfig {
	public static final String POPULAR_JOB_NAME = "popularAlcoholJob";
	private static final int CHUNK_SIZE = 100;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 인기 주류 선정 Job을 생성합니다.
	 */
	@Bean
	public Job popularAlcoholJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		Step popularStep = getPopularAlcoholStep(jobRepository, transactionManager);
		if (popularStep == null) {
			return null;
		}
		return new JobBuilder(POPULAR_JOB_NAME, jobRepository)
				.start(popularStep)
				.build();
	}

	/**
	 * 인기 주류 선정 스텝을 생성합니다.
	 */
	private Step getPopularAlcoholStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		String query = getQueryByResource();
		if (query == null) {
			log.error("인기 주류 선정 쿼리를 로드할 수 없습니다.");
			return null;
		}


		return new StepBuilder("popularAlcoholStep", jobRepository)
				.<PopularItemPayload, PopularItemPayload>chunk(CHUNK_SIZE, transactionManager)
				.reader(new PopularItemReader(jdbcTemplate, query))
				.processor(item -> item)
				.writer(this::savePopularItems)
				.build();
	}

	/**
	 * 인기 주류 선정을 위한 SQL 쿼리를 리소스 파일에서 로드합니다.
	 */
	private String getQueryByResource() {
		try {
			Resource resource = new ClassPathResource("storage/mysql/sql/popularity.sql");
			String query = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
			log.info("인기 주류 선정 쿼리 로드 완료: {}", resource.getDescription());
			return query;
		} catch (IOException e) {
			log.error("cant find popularity.sql files", e);
			return null;
		}
	}

	/**
	 * 계산된 인기 주류 데이터를 데이터베이스에 저장합니다.
	 */
	private void savePopularItems(Chunk<? extends PopularItemPayload> chunk) {
		if (chunk.isEmpty()) return;

		LocalDate today = LocalDate.now();
		String clearSql = "DELETE FROM popular_alcohols " +
				"WHERE year = ? AND month = ? AND day = ?";
		int deleted = jdbcTemplate.update(clearSql, today.getYear(), today.getMonthValue(), today.getDayOfMonth());
		log.debug("기존 인기 주류 데이터 삭제: {}", deleted);

		String sql = "INSERT INTO popular_alcohols (alcohol_id, year, month, day, review_score, rating_score, pick_score, popular_score, created_at) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		List<Object[]> batchArgs = chunk.getItems().stream()
				.map(item -> new Object[]{
						item.alcoholId(),
						item.year(),
						item.month(),
						item.day(),
						item.reviewScore(),
						item.ratingScore(),
						item.pickScore(),
						item.popularScore(),
						LocalDateTime.now()
				})
				.toList();

		int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
		int totalInserted = 0;
		for (int count : updateCounts) {
			totalInserted += count;
		}
		log.debug("인기 주류 데이터 삽입 완료: {}", totalInserted);
	}

	/**
	 * 인기 주류 데이터를 읽는 클래스입니다.
	 */
	private static class PopularItemReader implements ItemReader<PopularItemPayload> {
		private final JdbcTemplate jdbcTemplate;
		private final String query;
		private List<PopularItemPayload> results;
		private int currentIndex;

		public PopularItemReader(JdbcTemplate jdbcTemplate, String query) {
			this.jdbcTemplate = jdbcTemplate;
			this.query = query;
		}

		@Override
		public PopularItemPayload read() {
			if (results == null) {
				results = jdbcTemplate.query(query, new PopularItemPayload.PopularItemMapper());
				currentIndex = 0;
				log.debug("인기 주류 데이터 로드 완료: {} 건", results.size());
			}

			if (currentIndex >= results.size()) {
				return null;
			}

			return results.get(currentIndex++);
		}
	}

	@Component
	public static class PopularAlcoholQuartzJob extends BatchQuartzJob {
		public PopularAlcoholQuartzJob(JobLauncher jobLauncher, JobRegistry jobRegistry) {
			super(jobLauncher, jobRegistry, POPULAR_JOB_NAME, "popularAlcoholJob");
		}
	}

	@Builder
	public record PopularItemPayload(
		Long alcoholId,
		Integer year,
		Integer month,
		Integer day,
		BigDecimal reviewScore,
		BigDecimal ratingScore,
		BigDecimal pickScore,
		BigDecimal popularScore
	) {
		public static class PopularItemMapper implements RowMapper<PopularItemPayload> {
			@Override
			public PopularItemPayload mapRow(ResultSet rs, int rowNum) throws SQLException {
				LocalDate currentDate = LocalDate.now();
				return PopularItemPayload.builder()
					.alcoholId(rs.getLong("alcohol_id"))
					.year(currentDate.getYear())
					.month(currentDate.getMonthValue())
					.day(currentDate.getDayOfMonth())
					.reviewScore(rs.getObject("review_score") != null ? rs.getBigDecimal("review_score") : BigDecimal.ZERO)
					.ratingScore(rs.getObject("rating_score") != null ? rs.getBigDecimal("rating_score") : BigDecimal.ZERO)
					.pickScore(rs.getObject("pick_score") != null ? rs.getBigDecimal("pick_score") : BigDecimal.ZERO)
					.popularScore(rs.getObject("popular_score") != null ? rs.getBigDecimal("popular_score") : BigDecimal.ZERO)
					.build();
			}
		}
	}
}
