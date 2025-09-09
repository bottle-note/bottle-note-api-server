package app.bottlenote.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [infra] JpaAuditing")
class JpaAuditingIntegrationTest extends IntegrationTestSupport {

  private ReviewCreateRequest reviewCreateRequest;
  private OauthRequest oauthRequest;

  @Autowired private ReviewRepository reviewRepository;

  @BeforeEach
  void setUp() {
    oauthRequest = new OauthRequest("chadongmin@naver.com", null, SocialType.KAKAO, null, null);
    reviewCreateRequest = ReviewObjectFixture.getReviewCreateRequest();
  }

  @DisplayName("DB 저장 시 생성자와 수정자가 기록된다.")
  @Sql(
      scripts = {
        "/init-script/init-alcohol.sql",
        "/init-script/init-user.sql",
        "/init-script/init-review.sql",
        "/init-script/init-review-reply.sql"
      })
  @Test
  void test_1() throws Exception {
    // when
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(reviewCreateRequest))
                    .header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
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

    assertEquals(oauthRequest.email(), savedReview.getCreateBy());
  }
}
