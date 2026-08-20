package app.bottlenote.review.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] ReviewExplore 위치 정보")
class ReviewExploreLocationIntegrationTest extends IntegrationTestSupport {

  private static final String ENDPOINT = "/api/v1/reviews/explore/standard";

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;

  @Test
  @DisplayName("모든 위치 값이 있는 리뷰는 둘러보기 JSON에 locationInfo 전체를 반환한다")
  void 모든_위치_값이_있는_리뷰는_둘러보기_JSON에_locationInfo_전체를_반환한다() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    reviewTestFactory.persistReview(
        Review.builder()
            .userId(user.getId())
            .alcoholId(alcohol.getId())
            .content("위치전체계약")
            .reviewLocation(
                ReviewLocation.builder()
                    .name("도시술")
                    .zipCode("12345")
                    .address("서울 송파구 송파대로 145")
                    .detailAddress("2층")
                    .category("음식점 > 술집")
                    .mapUrl("https://example.com/place")
                    .latitude("37.0000")
                    .longitude("127.0000")
                    .build()));

    MvcTestResult result = explore("위치전체계약");

    result.assertThat().hasStatusOk();
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.locationName").isEqualTo("도시술");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.zipCode").isEqualTo("12345");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.address").isEqualTo("서울 송파구 송파대로 145");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.detailAddress").isEqualTo("2층");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.category").isEqualTo("음식점 > 술집");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.mapUrl").isEqualTo("https://example.com/place");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.latitude").isEqualTo("37.0000");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.longitude").isEqualTo("127.0000");
  }

  @Test
  @DisplayName("위치가 없는 리뷰는 둘러보기 JSON에 locationInfo null을 반환한다")
  void 위치가_없는_리뷰는_둘러보기_JSON에_locationInfo_null을_반환한다() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    reviewTestFactory.persistReview(
        Review.builder().userId(user.getId()).alcoholId(alcohol.getId()).content("위치없음계약"));

    MvcTestResult result = explore("위치없음계약");

    result.assertThat().hasStatusOk();
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo").isNull();
  }

  @Test
  @DisplayName("일부 위치 값만 있는 리뷰는 둘러보기 JSON에서 null 필드를 보존한다")
  void 일부_위치_값만_있는_리뷰는_둘러보기_JSON에서_null_필드를_보존한다() {
    User user = userTestFactory.persistUser();
    Alcohol alcohol = alcoholTestFactory.persistAlcohol();
    reviewTestFactory.persistReview(
        Review.builder()
            .userId(user.getId())
            .alcoholId(alcohol.getId())
            .content("위치부분계약")
            .reviewLocation(ReviewLocation.builder().name("도시술").address("서울 송파구").build()));

    MvcTestResult result = explore("위치부분계약");

    result.assertThat().hasStatusOk();
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.locationName").isEqualTo("도시술");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.address").isEqualTo("서울 송파구");
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.zipCode").isNull();
    result.assertThat().bodyJson().extractingPath("$.data.items[0].locationInfo.detailAddress").isNull();
  }

  private MvcTestResult explore(String keyword) {
    return mockMvcTester
        .get()
        .uri(ENDPOINT)
        .param("keywords", keyword)
        .contentType(APPLICATION_JSON)
        .with(csrf())
        .exchange();
  }
}
