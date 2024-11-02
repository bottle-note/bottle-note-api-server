package app.bottlenote.review.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.dto.request.LikesUpdateRequest;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] ReviewDomain Subquery")
@WithMockUser
@Sql(scripts = {
	"/init-script/init-alcohol.sql",
	"/init-script/init-user.sql",
	"/init-script/init-review.sql",
	"/init-script/init-review-reply.sql"
})
class ReviewSubqueryIntegrationTest extends IntegrationTestSupport {

	private final Long reviewId = 1L;
	private OauthRequest oauthRequest;
	private LikesUpdateRequest likeRequest;
	private LikesUpdateRequest dislikeRequest;
	private ReviewReplyRegisterRequest reviewReplyRegisterRequest;

	@BeforeEach
	void setUp() {
		oauthRequest = new OauthRequest("chadongmin@naver.com", SocialType.KAKAO, null, null);
		likeRequest = new LikesUpdateRequest(reviewId, LikeStatus.LIKE);
		dislikeRequest = new LikesUpdateRequest(reviewId, LikeStatus.DISLIKE);
		reviewReplyRegisterRequest = new ReviewReplyRegisterRequest("댓글입니다.", null);
	}

	@DisplayName("유저가 좋아요를 누르면 isLikedByMe는 true, likeCount는 1," +
		" 좋아요를 취소하면 isLikedByMe는 false, likeCount는 0이다.")
	@Test
	void testIsPickedAndLikeCountSubquery() throws Exception {
		//given
		mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(likeRequest))
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists());

		MvcResult resultAfterLike = mockMvc.perform(get("/api/v1/reviews/detail/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = resultAfterLike.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		ReviewDetailResponse reviewDetailResponse = mapper.convertValue(response.getData(), ReviewDetailResponse.class);

		assertEquals(1, reviewDetailResponse.reviewResponse().likeCount());
		assertEquals(Boolean.TRUE, reviewDetailResponse.reviewResponse().isLikedByMe());

		mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(dislikeRequest))
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists());


		MvcResult resultAfterDislike = mockMvc.perform(get("/api/v1/reviews/detail/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString2 = resultAfterDislike.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response2 = mapper.readValue(contentAsString2, GlobalResponse.class);
		ReviewDetailResponse reviewDetailResponse2 = mapper.convertValue(response2.getData(), ReviewDetailResponse.class);

		assertEquals(0, reviewDetailResponse2.reviewResponse().likeCount());
		assertEquals(Boolean.FALSE, reviewDetailResponse2.reviewResponse().isLikedByMe());
	}


	@DisplayName("유저가 댓글을 달면 hasReplyByMe는 true이고, replyCount는 1 증가한다.")
	@Test
	void reviewReplySubqueryTest() throws Exception {
	    // given
		MvcResult existResult = mockMvc.perform(get("/api/v1/reviews/detail/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString = existResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
		ReviewDetailResponse existReviewDetail = mapper.convertValue(response.getData(), ReviewDetailResponse.class);

		Long existReviewReplyCount = existReviewDetail.reviewResponse().replyCount();
		Boolean existHasReplyByMe = existReviewDetail.reviewResponse().hasReplyByMe();

		log.info("existReviewReplyCount : {}", existReviewReplyCount);
		log.info("existHasReplyByMe : {}", existHasReplyByMe);

		// when
		mockMvc.perform(post("/api/v1/review/reply/register/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(reviewReplyRegisterRequest))
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists());

	    // then
		MvcResult resultAfterRegisterReviewReply = mockMvc.perform(get("/api/v1/reviews/detail/{reviewId}", reviewId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + getToken(oauthRequest).getAccessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andReturn();

		String contentAsString2 = resultAfterRegisterReviewReply.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response2 = mapper.readValue(contentAsString2, GlobalResponse.class);
		ReviewDetailResponse ReviewDetailAfterRegisterReply = mapper.convertValue(response2.getData(), ReviewDetailResponse.class);

		Long reviewReplyCount = ReviewDetailAfterRegisterReply.reviewResponse().replyCount();
		Boolean hasReplyByMe = ReviewDetailAfterRegisterReply.reviewResponse().hasReplyByMe();

		log.info("afterReviewReplyCount : {}", reviewReplyCount);
		log.info("afterHasReplyByMe : {}", hasReplyByMe);

		assertEquals(existReviewReplyCount + 1, reviewReplyCount);
		assertEquals(Boolean.TRUE, hasReplyByMe);
	}


}
