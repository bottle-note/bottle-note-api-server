package app.bottlenote.like.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.dto.request.LikesUpdateRequest;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static app.bottlenote.like.dto.response.LikesUpdateResponse.Message.DISLIKE;
import static app.bottlenote.like.dto.response.LikesUpdateResponse.Message.LIKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] LikesController")
class LikesIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private LikesRepository likesRepository;

	@DisplayName("좋아요를 등록할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_1() throws Exception {

		LikesUpdateRequest likesUpdateRequest = new LikesUpdateRequest(1L, LikeStatus.LIKE);

		MvcResult result = mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(likesUpdateRequest))
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
		LikesUpdateResponse likesUpdateResponse = mapper.convertValue(response.getData(), LikesUpdateResponse.class);

		assertEquals(likesUpdateResponse.message(), LIKED.getMessage());
	}

	@DisplayName("좋아요를 해제 할 수 있다.")
	@Test
	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-alcohol.sql",
		"/init-script/init-review.sql"
	})
	void test_2() throws Exception {

		LikesUpdateRequest likesUpdateRequest = new LikesUpdateRequest(1L, LikeStatus.LIKE);
		LikesUpdateRequest dislikesUpdateRequest = new LikesUpdateRequest(1L, LikeStatus.DISLIKE);

		mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(likesUpdateRequest))
				.header("Authorization", "Bearer " + getToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists());

		Likes likes = likesRepository.findByReviewIdAndUserId(1L, getTokenUserId()).orElse(null);
		assertNotNull(likes);
		assertEquals(LikeStatus.LIKE, likes.getStatus());

		MvcResult result = mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(dislikesUpdateRequest))
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
		LikesUpdateResponse likesUpdateResponse = mapper.convertValue(response.getData(), LikesUpdateResponse.class);

		assertEquals(likesUpdateResponse.message(), DISLIKE.getMessage());
		Likes dislike = likesRepository.findByReviewIdAndUserId(1L, getTokenUserId()).orElse(null);
		assertNotNull(dislike);
		assertEquals(LikeStatus.DISLIKE, dislike.getStatus());
	}

}
