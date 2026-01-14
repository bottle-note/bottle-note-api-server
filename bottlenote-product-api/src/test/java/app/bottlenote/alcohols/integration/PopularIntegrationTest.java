package app.bottlenote.alcohols.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.fixture.AlcoholsViewHistoryTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] Popular API")
class PopularIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private AlcoholsViewHistoryTestFactory viewHistoryTestFactory;

  @Nested
  @DisplayName("주간 인기 API")
  class WeeklyPopularApi {

    @Test
    @DisplayName("주간 인기 위스키를 조회할 수 있다")
    void test_getPopularOfWeek() throws Exception {
      // given
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(5);
      for (int i = 0; i < alcohols.size(); i++) {
        alcoholTestFactory.persistPopularAlcohol(
            alcohols.get(i).getId(), BigDecimal.valueOf(0.5 - i * 0.1));
      }

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/popular/week")
              .param("top", "5")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      PopularsOfWeekResponse response = extractData(result, PopularsOfWeekResponse.class);
      assertNotNull(response);
      assertEquals(5, response.getAlcohols().size());
    }
  }

  @Nested
  @DisplayName("조회수 기반 인기 API")
  class ViewBasedPopularApi {

    @Test
    @DisplayName("주간 조회수 기반 인기 위스키를 조회할 수 있다")
    void test_getPopularViewWeekly() throws Exception {
      // given
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(5);
      List<User> users = createUsers(5);

      // 조회수 데이터 생성 (alcohol별로 다른 조회수)
      LocalDateTime now = LocalDateTime.now();
      for (int i = 0; i < alcohols.size(); i++) {
        Alcohol alcohol = alcohols.get(i);
        int viewCount = 5 - i;
        for (int j = 0; j < viewCount; j++) {
          viewHistoryTestFactory.persistAlcoholsViewHistory(
              users.get(j).getId(), alcohol.getId(), now.minusDays(j));
        }
      }

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/popular/view/week")
              .param("top", "5")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      PopularsOfWeekResponse response = extractData(result, PopularsOfWeekResponse.class);
      assertNotNull(response);
      assertEquals(5, response.getAlcohols().size());
      assertEquals(5, response.getTotalCount());
    }

    @Test
    @DisplayName("조회 기록이 부족하면 평점 높은 주류로 채워서 반환한다")
    void test_getPopularViewWeekly_fillWithRating() throws Exception {
      // given
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(10);
      List<User> users = createUsers(5);

      // 조회수 데이터 생성 (5개 주류만)
      LocalDateTime now = LocalDateTime.now();
      for (int i = 0; i < 5; i++) {
        Alcohol alcohol = alcohols.get(i);
        int viewCount = 5 - i;
        for (int j = 0; j < viewCount; j++) {
          viewHistoryTestFactory.persistAlcoholsViewHistory(
              users.get(j).getId(), alcohol.getId(), now.minusDays(j));
        }
      }

      // 평점 데이터 생성 (나머지 5개 주류)
      for (int i = 5; i < 10; i++) {
        ratingTestFactory.persistRating(users.get(0).getId(), alcohols.get(i).getId(), 5);
        ratingTestFactory.persistRating(users.get(1).getId(), alcohols.get(i).getId(), 4);
      }

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/popular/view/week")
              .param("top", "10")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      PopularsOfWeekResponse response = extractData(result, PopularsOfWeekResponse.class);
      assertNotNull(response);
      assertEquals(10, response.getTotalCount());
      assertTrue(response.getAlcohols().size() >= 5);
    }

    @Test
    @DisplayName("월간 조회수 기반 인기 위스키를 조회할 수 있다")
    void test_getPopularViewMonthly() throws Exception {
      // given
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(5);
      List<User> users = createUsers(5);

      // 조회수 데이터 생성 (월간 범위)
      LocalDateTime now = LocalDateTime.now();
      for (int i = 0; i < alcohols.size(); i++) {
        Alcohol alcohol = alcohols.get(i);
        int viewCount = 5 - i;
        for (int j = 0; j < viewCount; j++) {
          viewHistoryTestFactory.persistAlcoholsViewHistory(
              users.get(j).getId(), alcohol.getId(), now.minusDays(j * 7));
        }
      }

      // when
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/popular/view/monthly")
              .param("top", "5")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // then
      PopularsOfWeekResponse response = extractData(result, PopularsOfWeekResponse.class);
      assertNotNull(response);
      assertEquals(5, response.getAlcohols().size());
      assertEquals(5, response.getTotalCount());
    }
  }

  private List<User> createUsers(int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> userTestFactory.persistUser())
        .toList();
  }
}
