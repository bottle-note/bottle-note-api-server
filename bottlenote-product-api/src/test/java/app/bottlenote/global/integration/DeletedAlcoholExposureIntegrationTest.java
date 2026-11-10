package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.fixture.AlcoholsViewHistoryTestFactory;
import app.bottlenote.history.fixture.HistoryTestFactory;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] 삭제된 위스키의 연관 데이터 노출")
class DeletedAlcoholExposureIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private PicksTestFactory picksTestFactory;
  @Autowired private HistoryTestFactory historyTestFactory;
  @Autowired private AlcoholsViewHistoryTestFactory viewHistoryTestFactory;

  private User owner;
  private Alcohol alive;
  private Alcohol deleted;

  @BeforeEach
  void setUp() {
    owner = userTestFactory.persistUser();
    alive = alcoholTestFactory.persistAlcohol();
    deleted = alcoholTestFactory.persistDeletedAlcohol();
    for (Alcohol alcohol : List.of(alive, deleted)) {
      reviewTestFactory.persistReview(owner, alcohol);
      ratingTestFactory.persistRating(owner, alcohol, 4);
      picksTestFactory.persistPicks(alcohol.getId(), owner.getId(), PicksStatus.PICK);
      historyTestFactory.persistUserHistory(owner.getId(), EventType.IS_PICK, alcohol.getId());
      viewHistoryTestFactory.persistAlcoholsViewHistory(owner.getId(), alcohol.getId());
    }
  }

  @Test
  @DisplayName("마이페이지 개수와 마이보틀 목록에서 삭제된 위스키의 데이터를 제외한다")
  void 마이페이지에서_삭제된_위스키의_데이터를_제외한다() throws Exception {
    String myPage = get("/api/v1/my-page/" + owner.getId(), owner);

    assertThat(JsonPath.<Integer>read(myPage, "$.data.reviewCount")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(myPage, "$.data.ratingCount")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(myPage, "$.data.pickCount")).isEqualTo(1);
    for (String tab : List.of("reviews", "ratings", "picks")) {
      assertOnlyAlive(get("/api/v1/my-page/" + owner.getId() + "/my-bottle/" + tab, owner));
    }
  }

  @Test
  @DisplayName("리뷰 목록과 둘러보기에서 삭제된 위스키의 리뷰를 제외한다")
  void 리뷰_조회에서_삭제된_위스키의_리뷰를_제외한다() throws Exception {
    assertThat(reviewCount(get("/api/v1/reviews/" + alive.getId(), owner))).isEqualTo(1);
    assertThat(reviewCount(get("/api/v1/reviews/" + deleted.getId(), owner))).isZero();
    assertThat(reviewCount(get("/api/v1/reviews/me/" + alive.getId(), owner))).isEqualTo(1);
    assertThat(reviewCount(get("/api/v1/reviews/me/" + deleted.getId(), owner))).isZero();
    assertOnlyAlive(get("/api/v1/reviews/explore/standard", owner));
  }

  @Test
  @DisplayName("별점 후보 목록에서 삭제된 위스키를 제외한다")
  void 별점_후보에서_삭제된_위스키를_제외한다() throws Exception {
    User viewer = userTestFactory.persistUser();

    for (String sortType : List.of("RANDOM", "POPULAR")) {
      assertOnlyAlive(get("/api/v1/rating?sortType=" + sortType, viewer));
    }
  }

  @Test
  @DisplayName("활동 히스토리와 최근 본 위스키에서 삭제된 위스키를 제외한다")
  void 히스토리에서_삭제된_위스키를_제외한다() throws Exception {
    assertOnlyAlive(get("/api/v1/history/" + owner.getId(), owner));
    assertOnlyAlive(get("/api/v1/history/view/alcohols", owner));
  }

  private String get(String uri, User user) throws Exception {
    return mockMvcTester
        .get()
        .uri(uri)
        .header("Authorization", "Bearer " + getToken(user).accessToken())
        .exchange()
        .getResponse()
        .getContentAsString();
  }

  private void assertOnlyAlive(String body) {
    List<Integer> alcoholIds = JsonPath.read(body, "$..alcoholId");
    assertThat(alcoholIds).as(body).isNotEmpty().containsOnly(alive.getId().intValue());
  }

  private int reviewCount(String body) {
    return JsonPath.<List<Object>>read(body, "$.data.reviewList").size();
  }
}
