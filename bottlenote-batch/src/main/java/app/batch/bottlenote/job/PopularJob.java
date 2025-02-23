package app.batch.bottlenote.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PopularJob {
	public static final String POPULAR_JOB_NAME = "popularJob";
	private static final int CHUNK_SIZE = 10;
	private final JdbcTemplate jdbcTemplate;

	private String getQueryByResource() {
		try {
			// resources 디렉토리 하위의 파일 경로로 접근
			Resource resource = new ClassPathResource("popularity.sql");
			log.info("resource: {}", resource.getFilename());
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		} catch (IOException e) {
			log.error("cant find best-review-selected.sql files", e);
			return null;
		}
	}
}
