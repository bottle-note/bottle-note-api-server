package app.bottlenote.user.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.common.fixture.MyPageTestData;
import app.bottlenote.common.fixture.TestDataSetupHelper;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fixture.UserTestFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
class UserQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private ReviewTestFactory reviewTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;
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

      // 팔로잉 관계 생성
      for (User target : otherUsers) {
        userTestFactory.persistFollow(me, target);
      }

      // When
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/follow/{userId}/following-list", me.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf())
                      .header("Authorization", "Bearer " + token.accessToken()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      // Then
      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      FollowingSearchResponse followingSearchResponse =
          mapper.convertValue(globalResponse.getData(), FollowingSearchResponse.class);

      assertNotNull(followingSearchResponse);
      assertEquals(followingSearchResponse.totalCount(), otherUsers.size());
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

      // 팔로워 관계 생성 (다른 유저들이 나를 팔로우)
      for (User follower : followers) {
        userTestFactory.persistFollow(follower, me);
      }

      // When
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/follow/{userId}/follower-list", me.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf())
                      .header("Authorization", "Bearer " + token.accessToken()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      // Then
      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      FollowerSearchResponse followerSearchResponse =
          mapper.convertValue(globalResponse.getData(), FollowerSearchResponse.class);

      assertNotNull(followerSearchResponse);
      assertEquals(followerSearchResponse.totalCount(), followers.size());
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

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", targetUser.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .header("Authorization", "Bearer " + token.accessToken()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(targetUser.getId()))
          .andReturn();

      assertNotEquals(targetUser.getId(), me.getId());
    }

    @DisplayName("로그인 유저가 자신의 마이페이지를 조회할 수 있다.")
    @Test
    void test_2() throws Exception {
      // Given
      MyPageTestData data = testDataSetupHelper.setupMyPageTestData();
      User me = data.getUser(0);
      TokenItem token = getToken(me);

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", me.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .header("Authorization", "Bearer " + token.accessToken()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(me.getId()))
          .andExpect(jsonPath("$.data.isMyPage").value(true))
          .andReturn();
    }

    @DisplayName("비회원 유저가 타인의 마이페이지를 조회할 수 있다.")
    @Test
    void test_3() throws Exception {
      // Given
      MyPageTestData data = testDataSetupHelper.setupMyPageTestData();
      User targetUser = data.getUser(1);

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", targetUser.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(targetUser.getId()))
          .andReturn();
    }

    @DisplayName("유저가 존재하지 않는 경우 MYPAGE_NOT_ACCESSIBLE 에러를 발생한다.")
    @Test
    void test_4() throws Exception {
      // Given
      Error error = Error.of(UserExceptionCode.MYPAGE_NOT_ACCESSIBLE);
      final Long nonExistentUserId = 999999L;

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", nonExistentUserId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()));
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

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", targetUser.getId())
                  .param("keyword", "")
                  .param("regionId", "")
                  .param("sortType", "LATEST")
                  .param("sortOrder", "DESC")
                  .param("cursor", "0")
                  .param("pageSize", "50")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("Authorization", "Bearer " + token.accessToken())
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andReturn();

      assertNotEquals(targetUser.getId(), me.getId());
    }

    @DisplayName("비회원 유저는 조회하면 BAD_REQUEST 예외를 반환한다.")
    @Test
    void test_3() throws Exception {
      // Given
      User targetUser = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      reviewTestFactory.persistReview(targetUser, alcohol);

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", targetUser.getId())
                  .param("keyword", "")
                  .param("regionId", "")
                  .param("sortType", "LATEST")
                  .param("sortOrder", "DESC")
                  .param("cursor", "0")
                  .param("pageSize", "50")
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @DisplayName("마이보틀 유저가 존재하지 않는 경우 REQUIRED_USER_ID 예외를 반환한다.")
    @Test
    void test_4() throws Exception {
      // Given
      Error error = Error.of(UserExceptionCode.REQUIRED_USER_ID);
      final Long nonExistentUserId = 999999L;

      // When & Then
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", nonExistentUserId)
                  .param("keyword", "")
                  .param("regionId", "")
                  .param("sortType", "LATEST")
                  .param("sortOrder", "DESC")
                  .param("cursor", "0")
                  .param("pageSize", "50")
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()));
    }
  }
}
