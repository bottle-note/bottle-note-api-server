package app.bottlenote.user.integration;

import static app.bottlenote.user.constant.UserStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.ProfileImageChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fixture.UserTestFactory;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] UserBasicController")
@WithMockUser
class UserCommandIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserRepository userRepository;
  @Autowired private UserTestFactory userTestFactory;

  @DisplayName("회원탈퇴에 성공한다.")
  @Test
  void test_1() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .delete()
            .uri("/api/v1/users")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    // Then
    WithdrawUserResultResponse withdrawUserResultResponse =
        extractData(result, WithdrawUserResultResponse.class);

    userRepository
        .findById(withdrawUserResultResponse.userId())
        .ifPresent(withdraw -> assertEquals(DELETED, withdraw.getStatus()));
  }

  @DisplayName("탈퇴한 회원이 다시 탈퇴하는 경우 성공")
  @Test
  void test_2() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    Field statusField = User.class.getDeclaredField("status");
    statusField.setAccessible(true);
    statusField.set(user, UserStatus.DELETED);
    userRepository.save(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .delete()
            .uri("/api/v1/users")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    // Then
    result.assertThat().hasStatusOk();
  }

  @DisplayName("탈퇴한 회원이 재로그인 하는 경우 예외가 발생한다.")
  @Test
  void test_3() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    // 먼저 회원 탈퇴
    MvcTestResult deleteResult =
        mockMvcTester
            .delete()
            .uri("/api/v1/users")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .with(csrf())
            .exchange();

    deleteResult.assertThat().hasStatusOk();

    // When - 재로그인 시도
    MvcTestResult loginResult =
        mockMvcTester
            .post()
            .uri("/api/v1/oauth/login")
            .contentType(APPLICATION_JSON)
            .content(
                mapper.writeValueAsString(
                    new OauthRequest(user.getEmail(), null, SocialType.KAKAO, null, null)))
            .with(csrf())
            .exchange();

    // Then
    loginResult.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    loginResult
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[0].code")
        .isEqualTo(UserExceptionCode.USER_DELETED.name());
    loginResult
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo(UserExceptionCode.USER_DELETED.getMessage());
  }

  @DisplayName("닉네임 변경에 성공한다.")
  @Test
  void test_4() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .patch()
            .uri("/api/v1/users/nickname")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .content(mapper.writeValueAsString(new NicknameChangeRequest("newNickname")))
            .with(csrf())
            .exchange();

    // Then
    NicknameChangeResponse nicknameChangeResponse =
        extractData(result, NicknameChangeResponse.class);

    assertEquals("newNickname", nicknameChangeResponse.getChangedNickname());
  }

  @DisplayName("이미 존재하는 닉네임으로 변경할 수 없다.")
  @Test
  void test_5() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    User otherUser = userTestFactory.persistUserWithNickname("중복닉네임");
    TokenItem token = getToken(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .patch()
            .uri("/api/v1/users/nickname")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .content(mapper.writeValueAsString(new NicknameChangeRequest("중복닉네임")))
            .with(csrf())
            .exchange();

    // Then
    result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    result
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[0].code")
        .isEqualTo(UserExceptionCode.USER_NICKNAME_NOT_VALID.name());
    result
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[0].message")
        .isEqualTo(UserExceptionCode.USER_NICKNAME_NOT_VALID.getMessage());
  }

  @DisplayName("프로필 이미지 변경에 성공한다.")
  @Test
  void test_6() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .patch()
            .uri("/api/v1/users/profile-image")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .content(mapper.writeValueAsString(new ProfileImageChangeRequest("newProfileImageUrl")))
            .with(csrf())
            .exchange();

    // Then
    ProfileImageChangeResponse profileImageChangeResponse =
        extractData(result, ProfileImageChangeResponse.class);

    assertEquals("newProfileImageUrl", profileImageChangeResponse.profileImageUrl());
  }

  @DisplayName("프로필 이미지에 null을 넣는 경우 변경에 성공한다.(삭제)")
  @Test
  void test_7() throws Exception {
    // Given
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    // When
    MvcTestResult result =
        mockMvcTester
            .patch()
            .uri("/api/v1/users/profile-image")
            .contentType(APPLICATION_JSON)
            .header("Authorization", "Bearer " + token.accessToken())
            .content(mapper.writeValueAsString(new ProfileImageChangeRequest(null)))
            .with(csrf())
            .exchange();

    // Then
    ProfileImageChangeResponse profileImageChangeResponse =
        extractData(result, ProfileImageChangeResponse.class);

    assertNull(profileImageChangeResponse.profileImageUrl());
  }
}
