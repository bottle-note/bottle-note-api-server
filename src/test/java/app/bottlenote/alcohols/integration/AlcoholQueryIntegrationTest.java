package app.bottlenote.alcohols.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
						.contentType(APPLICATION_JSON)
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
						.contentType(APPLICATION_JSON)
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
		final String userEmail = "test1@example.com";
		final List<SocialType> userSocialType = List.of(SocialType.KAKAO);
		Alcohol alcohol = alcoholTestFactory.createAlcohol();
		final Long alcoholId = alcohol.getId();
		User user1 = alcoholTestFactory.createUser(1L, userEmail, "test1");
		User user2 = alcoholTestFactory.createUser(2L, "test@example.com", "test2");
		alcoholTestFactory.createFollow(user1, user2);
		ratingTestFactory.createRating(user2, alcohol, 3);
		OauthRequest oauthRequest = new OauthRequest(userEmail, null, userSocialType.get(0), null, null);
		TokenItem token = getToken(oauthRequest);
		MvcResult result = mockMvc.perform(get("/api/v1/alcohols/{alcoholId}", alcoholId)
						.contentType(APPLICATION_JSON)
						.header("Authorization", "Bearer " + token.accessToken())
						.with(csrf())
				)
				.andReturn();
		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		AlcoholDetailResponse alcoholDetail = mapper.convertValue(response.getData(), AlcoholDetailResponse.class);

		// then
		assertNotNull(alcoholDetail.friendsInfo());
		assertEquals(1, alcoholDetail.friendsInfo().getFollowerCount());
		assertEquals(user2.getId(), alcoholDetail.friendsInfo().getFriends().get(0).userId());
	}
}
