package app.bottlenote.user.integration;

import static app.bottlenote.user.constant.UserStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.ProfileImageChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.exception.UserExceptionCode;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserBasicController")
@WithMockUser
class UserCommandIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserRepository userRepository;

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("회원탈퇴에 성공한다.")
  @Test
  void test_1() throws Exception {
    // given
    MvcResult result =
        mockMvc
            .perform(
                delete("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    WithdrawUserResultResponse withdrawUserResultResponse =
        mapper.convertValue(response.getData(), WithdrawUserResultResponse.class);

    userRepository
        .findById(withdrawUserResultResponse.userId())
        .ifPresent(withdraw -> assertEquals(DELETED, withdraw.getStatus()));
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("탈퇴한 회원이 다시 탈퇴하는 경우 성공")
  @Test
  void test_2() throws Exception {
    // given
    final User firstUser = authSupport.getFirstUser();
    Field statusField = User.class.getDeclaredField("status");
    statusField.setAccessible(true);
    statusField.set(firstUser, UserStatus.DELETED);

    mockMvc
        .perform(
            delete("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getToken())
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").exists());
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("탈퇴한 회원이 재로그인 하는 경우 예외가 발생한다.")
  @Test
  void test_3() throws Exception {
    // given
    final User firstUser = authSupport.getFirstUser();

    mockMvc
        .perform(
            delete("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getToken())
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").exists());

    mockMvc
        .perform(
            post("/api/v1/oauth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        new OauthRequest(firstUser.getEmail(), null, SocialType.KAKAO, null, null)))
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].code").value(UserExceptionCode.USER_DELETED.name()))
        .andExpect(
            jsonPath("$.errors[0].message").value(UserExceptionCode.USER_DELETED.getMessage()));
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("닉네임 변경에 성공한다.")
  @Test
  void test_4() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                patch("/api/v1/users/nickname")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .content(mapper.writeValueAsString(new NicknameChangeRequest("newNickname")))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    NicknameChangeResponse nicknameChangeResponse =
        mapper.convertValue(response.getData(), NicknameChangeResponse.class);

    assertEquals("newNickname", nicknameChangeResponse.getChangedNickname());
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("이미 존재하는 닉네임으로 변경할 수 없다.")
  @Test
  void test_5() throws Exception {

    // 중복 닉네임 생성
    userRepository.save(
        User.builder()
            .email("test@email.com")
            .password("password")
            .nickName("fail")
            .role(UserType.ROLE_USER)
            .socialType(List.of(SocialType.KAKAO))
            .build());

    mockMvc
        .perform(
            patch("/api/v1/users/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getToken())
                .content(mapper.writeValueAsString(new NicknameChangeRequest("fail")))
                .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(
            jsonPath("$.errors[0].code").value(UserExceptionCode.USER_NICKNAME_NOT_VALID.name()))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value(UserExceptionCode.USER_NICKNAME_NOT_VALID.getMessage()));
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("프로필 이미지 변경에 성공한다.")
  @Test
  void test_6() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                patch("/api/v1/users/profile-image")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .content(
                        mapper.writeValueAsString(
                            new ProfileImageChangeRequest("newProfileImageUrl")))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    ProfileImageChangeResponse profileImageChangeResponse =
        mapper.convertValue(response.getData(), ProfileImageChangeResponse.class);

    assertEquals("newProfileImageUrl", profileImageChangeResponse.profileImageUrl());
  }

  @Sql(
      scripts = {"/init-script/init-user.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @DisplayName("프로필 이미지에 null을 넣는 경우 변경에 성공한다.(삭제)")
  @Test
  void test_7() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                patch("/api/v1/users/profile-image")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getToken())
                    .content(mapper.writeValueAsString(new ProfileImageChangeRequest(null)))
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists())
            .andReturn();

    String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    ProfileImageChangeResponse profileImageChangeResponse =
        mapper.convertValue(response.getData(), ProfileImageChangeResponse.class);

    assertNull(profileImageChangeResponse.profileImageUrl());
  }
}
