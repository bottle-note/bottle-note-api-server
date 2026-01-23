package app.bottlenote.like.integration;

import static app.bottlenote.like.dto.response.LikesUpdateResponse.Message.DISLIKE;
import static app.bottlenote.like.dto.response.LikesUpdateResponse.Message.LIKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.dto.request.LikesUpdateRequest;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] LikesController")
class LikesIntegrationTest extends IntegrationTestSupport {

  @Autowired private LikesRepository likesRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @DisplayName("좋아요를 등록할 수 있다.")
  @Test
  void test_1() throws Exception {
    // Given
    User reviewAuthor = userTestFactory.persistUser();
    User likeUser = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
    TokenItem token = getToken(likeUser);

    LikesUpdateRequest likesUpdateRequest = new LikesUpdateRequest(review.getId(), LikeStatus.LIKE);

    // When & Then
    MvcResult result =
        mockMvc
            .perform(
                put("/api/v1/likes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(likesUpdateRequest))
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    LikesUpdateResponse likesUpdateResponse =
        mapper.convertValue(response.getData(), LikesUpdateResponse.class);

    assertEquals(likesUpdateResponse.message(), LIKED.getMessage());
  }

  @DisplayName("좋아요를 해제 할 수 있다.")
  @Test
  void test_2() throws Exception {
    // Given
    User reviewAuthor = userTestFactory.persistUser();
    User likeUser = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    Review review = reviewTestFactory.persistReview(reviewAuthor, alcohol);
    TokenItem token = getToken(likeUser);

    LikesUpdateRequest likesUpdateRequest = new LikesUpdateRequest(review.getId(), LikeStatus.LIKE);
    LikesUpdateRequest dislikesUpdateRequest =
        new LikesUpdateRequest(review.getId(), LikeStatus.DISLIKE);

    // When - 좋아요 등록
    mockMvc
        .perform(
            put("/api/v1/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(likesUpdateRequest))
                .header("Authorization", "Bearer " + token.accessToken())
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").exists());

    Likes likes =
        likesRepository.findByReviewIdAndUserId(review.getId(), likeUser.getId()).orElse(null);
    assertNotNull(likes);
    assertEquals(LikeStatus.LIKE, likes.getStatus());

    // When - 좋아요 해제
    MvcResult result =
        mockMvc
            .perform(
                put("/api/v1/likes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dislikesUpdateRequest))
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    // Then
    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    LikesUpdateResponse likesUpdateResponse =
        mapper.convertValue(response.getData(), LikesUpdateResponse.class);

    assertEquals(likesUpdateResponse.message(), DISLIKE.getMessage());
    Likes dislike =
        likesRepository.findByReviewIdAndUserId(review.getId(), likeUser.getId()).orElse(null);
    assertNotNull(dislike);
    assertEquals(LikeStatus.DISLIKE, dislike.getStatus());
  }
}
