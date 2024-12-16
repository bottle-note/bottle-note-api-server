package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetail;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;


@Tag("integration")
@DisplayName("[integration] [controller] AlcoholQuery")
class AlcoholQueryControllerIntegrationTest extends IntegrationTestSupport {

	@Autowired
	AlcoholTestFactory alcoholTestFactory;

	@DisplayName("알코올 상세 조회 시 해당 알코올을 마셔본 팔로잉 유저의 정보를 조회할 수 있다.")
	@Sql(scripts = {
		"/init-script/init-alcohol.sql",
	})
	@Test
	void test_1() throws Exception {
		// given
		final Long alcoholId = 1L;
		final String userEmail = "test1@example.com";
		final List<SocialType> userSocialType = List.of(SocialType.KAKAO);

		User user1 = alcoholTestFactory.createUser(1L, userEmail, "test1");
		User user2 = alcoholTestFactory.createUser(2L, "test@example.com", "test2");
		alcoholTestFactory.createRating(user2, Alcohol.builder().id(1L).build(), 3);
		alcoholTestFactory.createFollow(user1, user2);

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
		AlcoholDetail alcoholDetail = mapper.convertValue(response.getData(), AlcoholDetail.class);

		// then
		assertNotNull(alcoholDetail.friendsInfo());
		assertEquals(1, alcoholDetail.friendsInfo().getFollowerCount());
		assertEquals(user2.getId(), alcoholDetail.friendsInfo().getFriends().get(0).userId());
	}
}
