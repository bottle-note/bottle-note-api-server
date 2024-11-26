package app.bottlenote.config;

import app.bottlenote.alcohols.repository.AlcoholQuerySupporter;
import app.bottlenote.global.data.serializers.CustomDeserializers;
import app.bottlenote.global.data.serializers.CustomDeserializers.TagListDeserializer;
import app.bottlenote.global.data.serializers.CustomSerializers;
import app.bottlenote.global.data.serializers.CustomSerializers.TagListSerializer;
import app.bottlenote.rating.repository.RatingQuerySupporter;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import app.bottlenote.user.repository.FollowQuerySupporter;
import app.bottlenote.user.repository.FollowerQuerySupporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;


@TestConfiguration
public class ModuleConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// JavaTimeModule 등록 (기존)
		objectMapper.registerModule(new JavaTimeModule());

		// 커스텀 모듈 등록
		SimpleModule customModule = new SimpleModule();
		customModule.addSerializer(LocalDateTime.class, new CustomSerializers.LocalDateTimeSerializer());
		customModule.addDeserializer(LocalDateTime.class, new CustomDeserializers.LocalDateTimeDeserializer());
		customModule.addSerializer(String.class, new TagListSerializer());
		customModule.addDeserializer(String.class, new TagListDeserializer());
		objectMapper.registerModule(customModule);
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
