package app.bottlenote.review.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[Integration] 리뷰 댓글 통합 테스트")
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewReplyIntegrationTest {

	private static final Logger log = LogManager.getLogger(ReviewReplyIntegrationTest.class);

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("리뷰의 최상위 댓글을 조회할 수 있다.")
	@Transactional(readOnly = true)
	void test_1() throws Exception {
		// given
		final Long reviewId = 4L;
		// final var oauthRequest = new OauthRequest("dev.bottle-note@gmail.com", SocialType.GOOGLE, GenderType.MALE, 25);
		// final String accessToken = oauthService.oauthLogin(oauthRequest).getAccessToken();

		// when && then
		MvcResult result = mockMvc.perform(get("/api/v1/review/reply/{reviewId}", reviewId)
				//.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.param("cursor", "0")
				.param("pageSize", "50")
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andReturn();

		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		List<ReviewReplyInfo> list = mapper.convertValue(response.getData(), mapper.getTypeFactory().constructCollectionType(List.class, ReviewReplyInfo.class));
		list.forEach(log::info);
	}
}
