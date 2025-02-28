package app.bottlenote.rating.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse.Message;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] PickController")
class RatingIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private AlcoholQueryRepository alcoholQueryRepository;
	@Autowired
	private RatingRepository ratingRepository;

	@DisplayName("별점을 등록할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	void test_1() throws Exception {

		RatingRegisterRequest ratingRegisterRequest = new RatingRegisterRequest(1L, 3.0);

		MvcResult result = mockMvc.perform(post("/api/v1/rating/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(ratingRegisterRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		RatingRegisterResponse ratingRegisterResponse = mapper.convertValue(response.getData(), RatingRegisterResponse.class);

		assertEquals(ratingRegisterRequest.rating().toString(), ratingRegisterResponse.rating());
		assertEquals(Message.SUCCESS.getMessage(), ratingRegisterResponse.message());
	}

	@DisplayName("별점 목록을 조회할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	void test_2() throws Exception {
		List<Alcohol> alcohols = alcoholQueryRepository.findAll();

		alcohols.forEach(
			a -> {
				double ratingPoint = (double) a.getId() % 5;
				Rating rating = Rating.builder()
					.id(RatingId.is(getTokenUserId(), a.getId()))
					.ratingPoint(RatingPoint.of(ratingPoint))
					.build();
				ratingRepository.save(rating);
			}
		);

		MvcResult result = mockMvc.perform(get("/api/v1/rating")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		RatingListFetchResponse ratingListFetchResponse = mapper.convertValue(response.getData(), RatingListFetchResponse.class);

		assertEquals(alcohols.size(), ratingListFetchResponse.totalCount());
	}

	@DisplayName("내가 매긴 특정 술의 별점을 조회할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	void test_3() throws Exception {
		List<Alcohol> alcohols = alcoholQueryRepository.findAll();

		alcohols.forEach(
			a -> {
				double ratingPoint = (double) a.getId() % 5;
				Rating rating = Rating.builder()
					.id(RatingId.is(getTokenUserId(), a.getId()))
					.ratingPoint(RatingPoint.of(ratingPoint))
					.build();
				ratingRepository.save(rating);
			}
		);

		MvcResult result = mockMvc.perform(get("/api/v1/rating/{alcoholId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		UserRatingResponse userRatingResponse = mapper.convertValue(response.getData(), UserRatingResponse.class);

		assertNotNull(userRatingResponse);
		assertEquals(1.0, userRatingResponse.rating());
		assertEquals(getTokenUserId(), userRatingResponse.userId());
		assertEquals(1, userRatingResponse.alcoholId());
	}
}
