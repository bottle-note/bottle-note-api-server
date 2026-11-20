package app.bottlenote.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.common.fixture.MyPageTestData;
import app.bottlenote.common.fixture.TestDataSetupHelper;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.RelationUserItem;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
class UserQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
  @Autowired private PicksTestFactory picksTestFactory;
  @Autowired private TestDataSetupHelper testDataSetupHelper;

  @Nested
  @DisplayName("팔로우/팔로잉")
  class Follower {

    @DisplayName("유저는 자신의 팔로잉 목록을 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // Given
      User me = userTestFactory.persistUser();
      List<User> otherUsers = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        otherUsers.add(userTestFactory.persistUser());
      }
      TokenItem token = getToken(me);

      for (User target : otherUsers) {
        userTestFactory.persistFollow(me, target);
      }

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/follow/{userId}/following-list", me.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .header("Authorization", "Bearer " + token.accessToken())
              .exchange();

      // Then
      FollowingSearchResponse followingSearchResponse =
          extractData(result, FollowingSearchResponse.class);

      assertNotNull(followingSearchResponse);
      assertEquals(followingSearchResponse.followingList().size(), otherUsers.size());
    }

    @DisplayName("유저는 자신을 팔로우하는 팔로워 목록을 조회할 수 있다.")
    @Test
    void test_2() throws Exception {
      // Given
      User me = userTestFactory.persistUser();
      List<User> followers = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        followers.add(userTestFactory.persistUser());
      }
      TokenItem token = getToken(me);

      for (User follower : followers) {
        userTestFactory.persistFollow(follower, me);
      }

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/follow/{userId}/follower-list", me.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .header("Authorization", "Bearer " + token.accessToken())
              .exchange();

      // Then
      FollowerSearchResponse followerSearchResponse =
          extractData(result, FollowerSearchResponse.class);

      assertNotNull(followerSearchResponse);
      assertEquals(followerSearchResponse.followerList().size(), followers.size());
    }

    @DisplayName("팔로잉·팔로워 목록을 조회할 때 별점 0은 평가 수에서 뺀다")
    @Test
    void followList_excludesZeroRatingFromRatingCount() throws Exception {
      // given
      User me = userTestFactory.persistUser();
      User friend = userTestFactory.persistUser();
      ratingTestFactory.persistRating(friend, alcoholTestFactory.persistAlcohol(), 4);
      ratingTestFactory.persistRating(friend, alcoholTestFactory.persistAlcohol(), 0);
      userTestFactory.persistFollow(me, friend);
      userTestFactory.persistFollow(friend, me);
      TokenItem token = getToken(me);

      // when
      FollowingSearchResponse following =
          extractData(
              mockMvcTester
                  .get()
                  .uri("/api/v1/follow/{userId}/following-list", me.getId())
                  .header("Authorization", "Bearer " + token.accessToken())
                  .exchange(),
              FollowingSearchResponse.class);
      FollowerSearchResponse followers =
          extractData(
              mockMvcTester
                  .get()
                  .uri("/api/v1/follow/{userId}/follower-list", me.getId())
                  .header("Authorization", "Bearer " + token.accessToken())
                  .exchange(),
              FollowerSearchResponse.class);

      // then
      assertThat(following.followingList())
          .singleElement()
          .extracting(RelationUserItem::ratingCount)
          .isEqualTo(1L);
      assertThat(followers.followerList())
          .singleElement()
          .extracting(RelationUserItem::ratingCount)
          .isEqualTo(1L);
    }

    @Test
    @DisplayName("자신과 타인의 팔로잉을 반환 커서로 끝까지 읽을 때 시간 동률과 관계 상태를 보존한다")
    void followingCursorPreservesOrderForSelfAndOther() throws Exception {
      User owner = userTestFactory.persistUser();
      User viewer = userTestFactory.persistUser();
      List<User> expected = seedCursorRelations(owner, true);
      for (User current : List.of(owner, viewer)) {
        assertCursorTraversal(
            owner, current, "following-list", "followingList", "followUserId", expected);
      }
    }

    @Test
    @DisplayName("자신과 타인의 팔로워를 반환 커서로 끝까지 읽을 때 시간 동률과 관계 상태를 보존한다")
    void followerCursorPreservesOrderForSelfAndOther() throws Exception {
      User owner = userTestFactory.persistUser();
      User viewer = userTestFactory.persistUser();
      List<User> expected = seedCursorRelations(owner, false);
      userTestFactory.persistFollow(owner, expected.get(1));
      for (User current : List.of(owner, viewer)) {
        assertCursorTraversal(owner, current, "follower-list", "followerList", "userId", expected);
      }
    }

    private List<User> seedCursorRelations(User owner, boolean following) {
      LocalDateTime tie = LocalDateTime.of(2026, 1, 1, 12, 0);
      List<User> related = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        User other =
            i == 2
                ? userTestFactory.persistUser(User.builder().status(UserStatus.DELETED))
                : userTestFactory.persistUser();
        related.add(other);
        Follow relation =
            following
                ? userTestFactory.persistFollow(owner, other)
                : userTestFactory.persistFollow(other, owner);
        LocalDateTime modified = i == 1 || i == 3 ? tie.plusDays(1) : tie;
        jdbc.update(
            "UPDATE follows SET last_modify_at = ? WHERE id = ?",
            Timestamp.valueOf(modified),
            relation.getId());
      }
      User unfollowed = userTestFactory.persistUser();
      Follow excluded =
          userTestFactory.persistFollow(
              Follow.builder()
                  .userId(following ? owner.getId() : unfollowed.getId())
                  .targetUserId(following ? unfollowed.getId() : owner.getId())
                  .status(FollowStatus.UNFOLLOW));
      jdbc.update(
          "UPDATE follows SET last_modify_at = ? WHERE id = ?",
          Timestamp.valueOf(tie.plusDays(2)),
          excluded.getId());
      return List.of(
          related.get(3), related.get(1), related.get(4), related.get(2), related.get(0));
    }

    private void assertCursorTraversal(
        User owner,
        User viewer,
        String endpoint,
        String listField,
        String idField,
        List<User> expected)
        throws Exception {
      String token = getToken(viewer).accessToken();
      List<Long> collected = new ArrayList<>();
      String cursor = null;
      for (int page = 0; page < 3; page++) {
        var request =
            mockMvcTester
                .get()
                .uri("/api/v1/follow/{userId}/" + endpoint, owner.getId())
                .param("size", "2")
                .contentType(APPLICATION_JSON)
                .with(csrf())
                .header("Authorization", "Bearer " + token);
        if (cursor != null) {
          request.param("cursor", cursor);
        }
        MvcTestResult result = request.exchange();
        result.assertThat().hasStatusOk();
        JsonNode body =
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        JsonNode items = body.path("data").path(listField);
        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(page < 2 ? 2 : 1);
        List<Long> pageIds = new ArrayList<>();
        for (JsonNode item : items) {
          Long id = item.path(idField).asLong();
          pageIds.add(id);
          String expectedStatus =
              endpoint.equals("following-list") || id.equals(expected.get(1).getId())
                  ? "FOLLOWING"
                  : "UNFOLLOW";
          assertThat(item.path("status").asText()).isEqualTo(expectedStatus);
        }
        int start = page * 2;
        assertThat(pageIds)
            .containsExactlyElementsOf(
                expected.subList(start, Math.min(start + 2, 5)).stream().map(User::getId).toList());
        collected.addAll(pageIds);
        JsonNode pagination = body.path("meta").path("pagination");
        assertThat(pagination.path("hasNext").isBoolean()).isTrue();
        assertThat(pagination.path("hasNext").asBoolean()).isEqualTo(page < 2);
        if (page < 2) {
          String next = pagination.path("nextCursor").asText();
          assertThat(next).isNotBlank().isNotEqualTo(cursor);
          cursor = next;
        } else {
          assertThat(pagination.has("nextCursor")).isTrue();
          assertThat(pagination.path("nextCursor").isNull()).isTrue();
        }
      }
      assertThat(collected)
          .containsExactlyElementsOf(expected.stream().map(User::getId).toList())
          .doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("마이페이지")
  class myPage {

    @DisplayName("로그인 유저가 타인의 마이페이지를 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // Given
      MyPageTestData data = testDataSetupHelper.setupMyPageTestData();
      User me = data.getUser(0);
      User targetUser = data.getUser(1);
      TokenItem token = getToken(me);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}", targetUser.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .header("Authorization", "Bearer " + token.accessToken())
              .exchange();

      // Then
      result.assertThat().hasStatusOk();
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.userId")
          .isEqualTo(targetUser.getId().intValue());

      assertNotEquals(targetUser.getId(), me.getId());
    }

    @DisplayName("로그인 유저가 자신의 마이페이지를 조회할 수 있다.")
    @Test
    void test_2() throws Exception {
      // Given
      MyPageTestData data = testDataSetupHelper.setupMyPageTestData();
      User me = data.getUser(0);
      TokenItem token = getToken(me);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}", me.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .header("Authorization", "Bearer " + token.accessToken())
              .exchange();

      // Then
      result.assertThat().hasStatusOk();
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.userId")
          .isEqualTo(me.getId().intValue());
      result.assertThat().bodyJson().extractingPath("$.data.isMyPage").isEqualTo(true);
    }

    @DisplayName("비회원 유저가 타인의 마이페이지를 조회할 수 있다.")
    @Test
    void test_3() throws Exception {
      // Given
      MyPageTestData data = testDataSetupHelper.setupMyPageTestData();
      User targetUser = data.getUser(1);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}", targetUser.getId())
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatusOk();
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.data.userId")
          .isEqualTo(targetUser.getId().intValue());
    }

    @DisplayName("유저가 존재하지 않는 경우 MYPAGE_NOT_ACCESSIBLE 에러를 발생한다.")
    @Test
    void test_4() throws Exception {
      // Given
      Error error = Error.of(UserExceptionCode.MYPAGE_NOT_ACCESSIBLE);
      final Long nonExistentUserId = 999999L;

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}", nonExistentUserId)
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatus(HttpStatus.FORBIDDEN);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(String.valueOf(error.code()));
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].status")
          .isEqualTo(error.status().name());
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].message")
          .isEqualTo(error.message());
    }
  }

  @Nested
  @DisplayName("마이보틀")
  class myBottle {

    @DisplayName("리뷰 마이보틀을 조회할 수 있다.")
    @Test
    void test_1() throws Exception {
      // Given
      User me = userTestFactory.persistUser();
      User targetUser = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      reviewTestFactory.persistReview(targetUser, alcohol);
      TokenItem token = getToken(me);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/reviews", targetUser.getId())
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", "LATEST")
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatusOk();
      assertNotEquals(targetUser.getId(), me.getId());
    }

    @DisplayName("마이보틀 교차 정렬 조건은 500 없이 조회할 수 있다.")
    @Test
    void getMyBottle_whenCrossSortTypeRequested_returnsOk() throws Exception {
      // Given
      User me = userTestFactory.persistUser();
      User targetUser = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      picksTestFactory.persistPicks(alcohol.getId(), targetUser.getId());
      reviewTestFactory.persistReview(targetUser, alcohol);
      ratingTestFactory.persistRating(targetUser, alcohol, 4);
      TokenItem token = getToken(me);

      // Then
      assertMyBottleStatusOk(token, targetUser, "picks", "RATING");
      assertMyBottleStatusOk(token, targetUser, "picks", "REVIEW");
      assertMyBottleStatusOk(token, targetUser, "reviews", "RATING");
      assertMyBottleStatusOk(token, targetUser, "ratings", "REVIEW");
    }

    @DisplayName("비회원 유저는 마이보틀 조회 시 401을 반환한다.")
    @Test
    void test_3() throws Exception {
      // Given
      User targetUser = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      reviewTestFactory.persistReview(targetUser, alcohol);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/reviews", targetUser.getId())
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", "LATEST")
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("비회원 유저는 별점 마이보틀 조회 시 401을 반환한다.")
    @Test
    void getRatingMyBottle_whenAnonymous_returnsUnauthorized() throws Exception {
      // Given
      User targetUser = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      ratingTestFactory.persistRating(targetUser, alcohol, 4);

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/ratings", targetUser.getId())
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", "LATEST")
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("비회원 유저는 찜 마이보틀 조회 시 401을 반환한다.")
    @Test
    void getPicksMyBottle_whenAnonymous_returnsUnauthorized() throws Exception {
      // Given
      User targetUser = userTestFactory.persistUser();

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/picks", targetUser.getId())
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", "LATEST")
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("인증 없이 마이보틀 유저가 존재하지 않는 경우 401을 반환한다.")
    @Test
    void test_4() throws Exception {
      // Given
      final Long nonExistentUserId = 999999L;

      // When
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/reviews", nonExistentUserId)
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", "LATEST")
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .with(csrf())
              .exchange();

      // Then
      result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void assertMyBottleStatusOk(
        TokenItem token, User targetUser, String tabPath, String sortType) throws Exception {
      MvcTestResult result =
          mockMvcTester
              .get()
              .uri("/api/v1/my-page/{userId}/my-bottle/" + tabPath, targetUser.getId())
              .param("keyword", "")
              .param("regionId", "")
              .param("sortType", sortType)
              .param("sortOrder", "DESC")
              .param("size", "50")
              .contentType(APPLICATION_JSON)
              .header("Authorization", "Bearer " + token.accessToken())
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();
    }
  }
}
