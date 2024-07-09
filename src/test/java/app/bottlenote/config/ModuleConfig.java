package app.bottlenote.config;

import app.bottlenote.alcohols.repository.AlcoholQuerySupporter;
import app.bottlenote.follow.repository.FollowQuerySupporter;
import app.bottlenote.follow.repository.FollowerQuerySupporter;
import app.bottlenote.rating.repository.RatingQuerySupporter;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@TestConfiguration
public class ModuleConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}

	@Bean
	public AlcoholQuerySupporter alcoholQuerySupporter() {
		return new AlcoholQuerySupporter();
	}

	@Bean
	public FollowerQuerySupporter followerQuerySupporter() {
		return new FollowerQuerySupporter();
	}

	@Bean
	public FollowQuerySupporter followQuerySupporter() {
		return new FollowQuerySupporter();
	}

	@Bean
	public ReviewQuerySupporter reviewQuerySupporter() {
		return new ReviewQuerySupporter();
	}

	@Bean
	public RatingQuerySupporter ratingQuerySupporter() {
		return new RatingQuerySupporter();
	}
}
