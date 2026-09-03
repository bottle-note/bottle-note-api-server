package app.bottlenote.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.RatingMyBottleItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] 마이보틀 별점 노출")
class RatingMyBottleDisplayIntegrationTest extends IntegrationTestSupport {

  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("마이보틀 평균 별점은 소수점 첫째 자리 숫자로 4.0, 4.5, 4.6을 노출한다")
  void 마이보틀_평균_별점은_소수점_첫째_자리로_노출한다() throws Exception {
    User owner = userTestFactory.persistUser();
    Alcohol exact = alcoholTestFactory.persistAlcoholWithName("마이보틀정수", "MyBottle Integer");
    Alcohol half = alcoholTestFactory.persistAlcoholWithName("마이보틀반점", "MyBottle Half");
    Alcohol decimal = alcoholTestFactory.persistAlcoholWithName("마이보틀소수", "MyBottle Decimal");
    persistRatings(exact, owner, 4.0, 4.0);
    persistRatings(half, owner, 4.0, 5.0);
    persistRatings(decimal, owner, 4.0, 4.5, 5.0, 5.0);

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri("/api/v1/my-page/{userId}/my-bottle/ratings", owner.getId())
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + getToken(owner).accessToken())
            .param("size", "10")
            .with(csrf())
            .exchange();

    result.assertThat().hasStatusOk();
    MyBottleResponse response = extractData(result, MyBottleResponse.class);
    Map<Long, Double> averageByAlcoholId =
        response.myBottleList().stream()
            .map(item -> mapper.convertValue(item, RatingMyBottleItem.class))
            .collect(
                Collectors.toMap(
                    item -> item.baseMyBottleInfo().alcoholId(),
                    RatingMyBottleItem::averageRatingPoint));

    assertThat(averageByAlcoholId.get(exact.getId())).isEqualTo(4.0);
    assertThat(averageByAlcoholId.get(half.getId())).isEqualTo(4.5);
    assertThat(averageByAlcoholId.get(decimal.getId())).isEqualTo(4.6);
  }

  private void persistRatings(Alcohol alcohol, User owner, double ownerPoint, double... others) {
    persistRating(owner, alcohol, ownerPoint);
    for (double point : others) {
      persistRating(userTestFactory.persistUser(), alcohol, point);
    }
  }

  private void persistRating(User user, Alcohol alcohol, double point) {
    ratingTestFactory.persistRating(
        Rating.builder()
            .id(Rating.RatingId.is(user.getId(), alcohol.getId()))
            .ratingPoint(RatingPoint.of(point)));
  }
}
