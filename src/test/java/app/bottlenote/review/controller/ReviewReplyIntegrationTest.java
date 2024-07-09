package app.bottlenote.review.controller;

import app.bottlenote.review.dto.request.ReviewReplyPageableRequest;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.service.OauthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewReplyIntegrationTest {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OauthService oauthService;

	@Test
	@Transactional(readOnly = true)
	void test_1() throws Exception {
		// given
		final Long reviewId = 4L;
		final var request = new ReviewReplyPageableRequest(null, null);
		final var oauthRequest = new OauthRequest("dev.bottle-note@gmail.com", SocialType.GOOGLE, GenderType.MALE, 25);
		final String accessToken = oauthService.oauthLogin(oauthRequest).getAccessToken();

		// when
		mockMvc.perform(get("/api/v1/review/reply/{reviewId}", reviewId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk());
		// then
	}
}
