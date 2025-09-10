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
import app.bottlenote.shared.data.response.Error;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.FollowRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
class UserQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private FollowRepository followRepository;
  @Autowired private UserRepository userRepository;

  @Nested
  @DisplayName("팔로우/팔로잉")
  class Follower {

    @DisplayName("유저는 자신의 팔로잉 목록을 조회할 수 있다.")
    @Sql(scripts = {"/init-script/init-user.sql"})
    @Test
    void test_1() throws Exception {

      final Long tokenUserId = getTokenUserId();

      List<User> allUsers =
          userRepository.findAll().stream()
              .filter(userId -> !userId.getId().equals(tokenUserId))
              .toList();

      allUsers.forEach(
          u -> {
            Follow follow =
                Follow.builder()
                    .userId(tokenUserId)
                    .targetUserId(u.getId())
                    .status(FollowStatus.FOLLOWING)
                    .build();
            followRepository.save(follow);
          });

      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/follow/{userId}/following-list", tokenUserId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf())
                      .header("Authorization", "Bearer " + getToken()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      FollowingSearchResponse followingSearchResponse =
          mapper.convertValue(globalResponse.getData(), FollowingSearchResponse.class);

      assertNotNull(followingSearchResponse);
      assertEquals(followingSearchResponse.totalCount(), allUsers.size());
    }

    @DisplayName("유저는 자신을 팔로우하는 팔로워 목록을 조회할 수 있다.")
    @Sql(scripts = {"/init-script/init-user.sql"})
    @Test
    void test_2() throws Exception {

      final Long tokenUserId = getTokenUserId();

      List<User> allUsers =
          userRepository.findAll().stream()
              .filter(userId -> !userId.getId().equals(tokenUserId))
              .toList();

      allUsers.forEach(
          u -> {
            Follow follow =
                Follow.builder()
                    .userId(u.getId())
                    .targetUserId(tokenUserId)
                    .status(FollowStatus.FOLLOWING)
                    .build();
            followRepository.save(follow);
          });

      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/follow/{userId}/follower-list", tokenUserId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf())
                      .header("Authorization", "Bearer " + getToken()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      FollowerSearchResponse followerSearchResponse =
          mapper.convertValue(globalResponse.getData(), FollowerSearchResponse.class);

      assertNotNull(followerSearchResponse);
      assertEquals(followerSearchResponse.totalCount(), allUsers.size());
    }
  }

  @Nested
  @DisplayName("마이페이지")
  class myPage {

    @DisplayName("로그인 유저가 타인의 마이페이지를 조회할 수 있다.")
    @Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
    @Test
    void test_1() throws Exception {

      String accessToken = getToken();
      Long userId = 2L;
      Long requestUserId = getTokenUserId();

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .header("Authorization", "Bearer " + accessToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(userId))
          .andReturn();

      assertNotEquals(userId, requestUserId);
    }

    @DisplayName("로그인 유저가 자신의 마이페이지를 조회할 수 있다.")
    @Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
    @Test
    void test_2() throws Exception {

      String accessToken = getToken();
      Long userId = getTokenUserId();

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .header("Authorization", "Bearer " + accessToken))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(userId))
          .andExpect(jsonPath("$.data.isMyPage").value(true))
          .andReturn();
    }

    @DisplayName("비회원 유저가 타인의 마이페이지를 조회할 수 있다.")
    @Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
    @Test
    void test_3() throws Exception {

      final Long userId = 2L;

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andExpect(jsonPath("$.data.userId").value(userId))
          .andReturn();
    }

    @DisplayName("유저가 존재하지 않는 경우 MYPAGE_NOT_ACCESSIBLE 에러를 발생한다.")
    @Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
    @Test
    void test_4() throws Exception {
      Error error = Error.of(UserExceptionCode.MYPAGE_NOT_ACCESSIBLE);
      final Long userId = 999L; // 존재하지 않는 유저 ID
      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}", userId)
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
    @Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
    @Test
    void test_1() throws Exception {

      String accessToken = getToken();
      Long userId = 2L;
      Long requestUserId = getTokenUserId();

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", userId)
                  .param("keyword", "")
                  .param("regionId", "")
                  .param("sortType", "LATEST")
                  .param("sortOrder", "DESC")
                  .param("cursor", "0")
                  .param("pageSize", "50")
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("Authorization", "Bearer " + accessToken)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists())
          .andReturn();

      assertNotEquals(userId, requestUserId);
    }

    @DisplayName("비회원 유저는 조회하면 BAD_REQUEST 예외를 반환한다.")
    @Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
    @Test
    void test_3() throws Exception {

      final Long userId = 2L;

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", userId)
                  .param("keyword", "")
                  .param("regionId", "")
                  .param("sortType", "LATEST")
                  .param("sortOrder", "DESC")
                  .param("cursor", "0")
                  .param("pageSize", "50")
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest()); // 비회원은 접근 불가
    }

    @DisplayName("마이보틀 유저가 존재하지 않는 경우 REQUIRED_USER_ID 예외를 반환한다.")
    @Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
    @Test
    void test_4() throws Exception {
      Error error = Error.of(UserExceptionCode.REQUIRED_USER_ID);
      final Long userId = 999L; // 존재하지 않는 유저 ID

      mockMvc
          .perform(
              get("/api/v1/my-page/{userId}/my-bottle/reviews", userId)
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
