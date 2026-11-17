package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.AlcoholDeleteGuard;
import app.bottlenote.alcohols.domain.AlcoholDetailQueryRepository;
import app.bottlenote.alcohols.domain.AlcoholPickStatusReader;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.alcohols.repository.QueryDslAlcoholDetailQueryRepository;
import app.bottlenote.global.config.jpa.AuditorAwareImpl;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.picks.service.DefaultAlcoholPickStatusReader;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.rating.service.RatingAlcoholDeleteGuard;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.review.service.ReviewAlcoholDeleteGuard;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.AuditorAware;

@Tag("integration")
@DisplayName("[integration] Product 주류 경계 조립과 찜·조회 효과")
class AppAssemblyAlcoholBoundaryIntegrationTest extends IntegrationTestSupport {
  @Autowired private ApplicationContext context;
  @Autowired private UserTestFactory users;
  @Autowired private AlcoholTestFactory alcohols;
  @Autowired private ReviewTestFactory reviews;
  @Autowired private PicksTestFactory picks;
  @Autowired private RatingTestFactory ratings;

  @Test
  @DisplayName("Product 앱을 조립할 때 주류 경계의 역할마다 정해진 구현을 정해진 순서로 등록한다")
  void context_registersAlcoholBoundaryRoles() {
    // when & then
    assertThat(rolesOf(AuditorAware.class)).containsExactly(AuditorAwareImpl.class);
    assertThat(rolesOf(AlcoholPickStatusReader.class))
        .containsExactly(DefaultAlcoholPickStatusReader.class);
    assertThat(rolesOf(AlcoholDetailQueryRepository.class))
        .containsExactly(QueryDslAlcoholDetailQueryRepository.class);
    assertThat(rolesOf(AlcoholDeleteGuard.class))
        .containsExactly(ReviewAlcoholDeleteGuard.class, RatingAlcoholDeleteGuard.class);
  }

  @Test
  @DisplayName("리뷰 상세를 조회할 때 조회자의 찜 상태에 따라 주류 정보의 isPicked를 채운다")
  void reviewDetail_fillsIsPickedByViewerPickStatus() throws Exception {
    // given
    var author = users.persistUser();
    var picker = users.persistUser();
    var unpicker = users.persistUser();
    var alcohol = alcohols.persistAlcohol();
    var review = reviews.persistReview(author, alcohol);
    picks.persistPicks(alcohol.getId(), picker.getId(), PicksStatus.PICK);
    picks.persistPicks(alcohol.getId(), unpicker.getId(), PicksStatus.UNPICK);

    // when
    JsonNode picked = reviewAlcoholInfo(review.getId(), picker);
    JsonNode unpicked = reviewAlcoholInfo(review.getId(), unpicker);
    JsonNode neverPicked = reviewAlcoholInfo(review.getId(), author);
    JsonNode guest = reviewAlcoholInfo(review.getId(), null);

    // then
    assertThat(picked.path("alcoholId").asLong()).isEqualTo(alcohol.getId());
    assertThat(picked.path("korName").asText()).isEqualTo(alcohol.getKorName());
    assertThat(picked.path("isPicked").isBoolean()).isTrue();
    assertThat(picked.path("isPicked").booleanValue()).isTrue();
    for (JsonNode notPicked : List.of(unpicked, neverPicked, guest)) {
      assertThat(notPicked.path("alcoholId").asLong()).isEqualTo(alcohol.getId());
      assertThat(notPicked.path("isPicked").isBoolean()).isTrue();
      assertThat(notPicked.path("isPicked").booleanValue()).isFalse();
    }
  }

  @Test
  @DisplayName("삭제된 주류의 리뷰 상세를 조회할 때 찜 여부와 관계없이 주류 정보를 비워 응답한다")
  void reviewDetail_ofDeletedAlcohol_returnsEmptyAlcoholInfo() throws Exception {
    // given
    var author = users.persistUser();
    var alcohol = alcohols.persistDeletedAlcohol();
    var review = reviews.persistReview(author, alcohol);
    picks.persistPicks(alcohol.getId(), author.getId(), PicksStatus.PICK);

    // when
    JsonNode info = reviewAlcoholInfo(review.getId(), author);

    // then
    assertThat(info.path("alcoholId").isNull()).isTrue();
    assertThat(info.path("isPicked").isNull()).isTrue();
  }

  @Test
  @DisplayName("주류 상세를 조회할 때 조회자의 찜과 별점을 집계 값과 함께 응답한다")
  void alcoholDetail_returnsViewerPickAndRatingWithAggregates() throws Exception {
    // given
    var member = users.persistUser();
    var alcohol = alcohols.persistAlcohol();
    picks.persistPicks(alcohol.getId(), member.getId(), PicksStatus.PICK);
    ratings.persistRating(member.getId(), alcohol.getId(), 4);

    // when
    JsonNode asMember = alcoholDetail(alcohol.getId(), member);
    JsonNode asGuest = alcoholDetail(alcohol.getId(), null);

    // then
    assertThat(asMember.path("isPicked").booleanValue()).isTrue();
    assertThat(asMember.path("myRating").asDouble()).isEqualTo(4.0);
    assertThat(asGuest.path("isPicked").isBoolean()).isTrue();
    assertThat(asGuest.path("isPicked").booleanValue()).isFalse();
    assertThat(asGuest.path("myRating").asDouble()).isEqualTo(0.0);
    for (JsonNode detail : List.of(asMember, asGuest)) {
      assertThat(detail.path("alcoholId").asLong()).isEqualTo(alcohol.getId());
      assertThat(detail.path("rating").asDouble()).isEqualTo(4.0);
      assertThat(detail.path("totalRatingsCount").asLong()).isEqualTo(1L);
      assertThat(detail.path("pickCount").asLong()).isEqualTo(1L);
      assertThat(detail.path("reviewCount").asLong()).isZero();
    }
  }

  private List<Class<?>> rolesOf(Class<?> role) {
    return context
        .getBeanProvider(role)
        .orderedStream()
        .<Class<?>>map(AopUtils::getTargetClass)
        .toList();
  }

  private JsonNode reviewAlcoholInfo(Long reviewId, User viewer) throws Exception {
    return get("/api/v1/reviews/detail/{id}", reviewId, viewer).path("alcoholInfo");
  }

  private JsonNode alcoholDetail(Long alcoholId, User viewer) throws Exception {
    return get("/api/v1/alcohols/{id}", alcoholId, viewer).path("alcohols");
  }

  /** viewer가 null이면 인증 헤더 없이 게스트로 호출한다. */
  private JsonNode get(String uri, Long id, User viewer) throws Exception {
    var request = mockMvcTester.get().uri(uri, id);
    if (viewer != null) {
      request = request.header("Authorization", "Bearer " + getToken(viewer).accessToken());
    }
    var response = request.exchange();
    response.assertThat().hasStatusOk();
    return mapper.readTree(response.getResponse().getContentAsString()).path("data");
  }
}
