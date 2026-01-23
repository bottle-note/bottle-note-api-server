package app.bottlenote.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
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
@DisplayName("[integration] [infra] JpaAuditing")
class JpaAuditingIntegrationTest extends IntegrationTestSupport {

  @Autowired private ReviewRepository reviewRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;

  @DisplayName("DB 저장 시 생성자와 수정자가 기록된다.")
  @Test
  void test_1() throws Exception {
    // given
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    TokenItem token = getToken(user);

    ReviewCreateRequest reviewCreateRequest =
        ReviewObjectFixture.getReviewCreateRequestWithAlcoholId(alcohol.getId());

    // when
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(reviewCreateRequest))
                    .header("Authorization", "Bearer " + token.accessToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    Review review =
        mapper.convertValue(
            response.getData(), mapper.getTypeFactory().constructType(Review.class));

    Review savedReview = reviewRepository.findById(review.getId()).orElseGet(null);

    assertEquals(user.getEmail(), savedReview.getCreateBy());
  }
}
