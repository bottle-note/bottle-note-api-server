package app.bottlenote.rating.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.dto.request.RatingRegisterRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse.Message;
import app.bottlenote.rating.dto.response.UserRatingResponse;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] RatingController")
class RatingIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;

  @DisplayName("별점을 등록할 수 있다.")
  @Test
  void test_1() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TokenItem token = getToken(user);

    RatingRegisterRequest ratingRegisterRequest = new RatingRegisterRequest(alcohol.getId(), 3.0);

    // When & Then
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/rating/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(ratingRegisterRequest))
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
    RatingRegisterResponse ratingRegisterResponse =
        mapper.convertValue(response.getData(), RatingRegisterResponse.class);

    assertEquals(ratingRegisterRequest.rating().toString(), ratingRegisterResponse.rating());
    assertEquals(Message.SUCCESS.getMessage(), ratingRegisterResponse.message());
  }

  @DisplayName("별점 목록을 조회할 수 있다.")
  @Test
  void test_2() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    List<Alcohol> alcohols = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      alcohols.add(alcoholTestFactory.persistAlcohol());
    }
    TokenItem token = getToken(user);

    // 각 알코올에 별점 등록
    for (int i = 0; i < alcohols.size(); i++) {
      int ratingPoint = (i % 5) + 1;
      ratingTestFactory.persistRating(user, alcohols.get(i), ratingPoint);
    }

    // When
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/rating")
                    .contentType(MediaType.APPLICATION_JSON)
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
    RatingListFetchResponse ratingListFetchResponse =
        mapper.convertValue(response.getData(), RatingListFetchResponse.class);

    assertEquals(alcohols.size(), ratingListFetchResponse.totalCount());
  }

  @DisplayName("내가 매긴 특정 술의 별점을 조회할 수 있다.")
  @Test
  void test_3() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TokenItem token = getToken(user);

    // 별점 1점 등록
    ratingTestFactory.persistRating(user, alcohol, 1);

    // When
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/rating/{alcoholId}", alcohol.getId())
                    .contentType(MediaType.APPLICATION_JSON)
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
    UserRatingResponse userRatingResponse =
        mapper.convertValue(response.getData(), UserRatingResponse.class);

    assertNotNull(userRatingResponse);
    assertEquals(1.0, userRatingResponse.rating());
    assertEquals(user.getId(), userRatingResponse.userId());
    assertEquals(alcohol.getId(), userRatingResponse.alcoholId());
  }
}
