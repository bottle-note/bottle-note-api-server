package app.bottlenote.alcohols.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@Tag("integration")
@DisplayName("[integration] [controller] AlcoholQuery")
class AlcoholQueryIntegrationTest extends IntegrationTestSupport {

	@Autowired
	AlcoholQueryRepository alcoholQueryRepository;
	@Autowired
	AlcoholTestFactory alcoholTestFactory;
	@Autowired
	RatingTestFactory ratingTestFactory;

	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql"
	})
	@DisplayName("알코올 목록조회를 할 수 있다.")
	@Test
	void test_1() throws Exception {

		MvcResult result = mockMvc.perform(get("/api/v1/alcohols/search")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andReturn();
		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		AlcoholSearchResponse alcoholSearchResponse = mapper.convertValue(response.getData(), AlcoholSearchResponse.class);

		List<Alcohol> alcohols = alcoholQueryRepository.findAll();
		assertNotNull(alcoholSearchResponse);
		assertEquals(alcohols.size(), alcoholSearchResponse.getTotalCount());
	}

	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	@DisplayName("알코올 상세 조회를 할 수 있다.")
	@Test
	void test_2() throws Exception {
		final Long alcoholId = 1L;

		MvcResult result = mockMvc.perform(get("/api/v1/alcohols/{alcoholId}", alcoholId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andReturn();
		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		AlcoholDetailResponse alcoholDetail = mapper.convertValue(response.getData(), AlcoholDetailResponse.class);

		assertNotNull(alcoholDetail.alcohols());
		assertNotNull(alcoholDetail.reviewInfo());
		assertNotNull(alcoholDetail.friendsInfo());
	}

	@DisplayName("알코올 상세 조회 시 해당 알코올을 마셔본 팔로잉 유저의 정보를 조회할 수 있다.")
	@Test
	void test_3() throws Exception {
		// given
		final Long alcoholId = 1L;
		final String userEmail = "test1@example.com";
		final List<SocialType> userSocialType = List.of(SocialType.KAKAO);

		Alcohol alcohol = alcoholTestFactory.createAlcohol();
		User user1 = alcoholTestFactory.createUser(1L, userEmail, "test1");
		User user2 = alcoholTestFactory.createUser(2L, "test@example.com", "test2");
		alcoholTestFactory.createFollow(user1, user2);
		ratingTestFactory.createRating(user2, alcohol, 3);

		MvcResult authResult = mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(new OauthRequest(userEmail, userSocialType.get(0), null, null)))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andReturn();
		String contentAsString = authResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse tokenResponse = mapper.readValue(contentAsString, GlobalResponse.class);
		JsonNode dataNode = mapper.convertValue(tokenResponse.getData(), JsonNode.class);
		String accessToken = dataNode.get("accessToken").asText();

		MvcResult result = mockMvc.perform(get("/api/v1/alcohols/{alcoholId}", alcoholId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + accessToken)
				.with(csrf())
			)
			.andDo(print())
			.andReturn();
		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		AlcoholDetailResponse alcoholDetail = mapper.convertValue(response.getData(), AlcoholDetailResponse.class);

		// then
		assertNotNull(alcoholDetail.friendsInfo());
		assertEquals(1, alcoholDetail.friendsInfo().getFollowerCount());
		assertEquals(user2.getId(), alcoholDetail.friendsInfo().getFriends().get(0).userId());
	}

	/**
	 * TODO : 추가해야 할 테스트 목록
	 * 1. 노출 예상 사용자지만 사용자가 블럭됬을 경우
	 * 2 노출 예상 사용자지만 사용자가 탈퇴한 경우(논리적 삭제 상태)
	 * 3. 외 기본적인 오류 상황
	 */
}
