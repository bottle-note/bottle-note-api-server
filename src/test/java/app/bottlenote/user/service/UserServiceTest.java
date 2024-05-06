package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.common.jwt.JwtTokenProvider;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.TokenRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@DisplayName("유저 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private OauthService oauthService;
	@Mock
	private NicknameGenerator nicknameGenerator;
	@Mock
	private OauthRepository oauthRepository;
	@Mock
	private JwtTokenProvider jwtTokenProvider;


	private final OauthRequest request = OauthRequest.builder()
		.age(26)
		.email("cdm2883@naver.com")
		.socialType(SocialType.KAKAO)
		.gender(GenderType.MALE)
		.build();

	private final String nickName = "mockNickname";

	private final User user = User.builder()
		.id(1L)
		.email("cdm2883@naver.com")
		.gender("남")
		.socialType(SocialType.KAKAO)
		.age(26)
		.nickName(nickName)
		.role(UserType.ROLE_USER)
		.build();

	private final OauthResponse oauthResponse = OauthResponse.builder()
		.accessToken("mock-accessToken")
		.refreshToken("mock-refreshToken")
		.build();

	@Test
	@DisplayName("회원가입을 할 수 있다.")
	void signup_test() {

		//given

		//when
		when(nicknameGenerator.generateNickname()).thenReturn(nickName);

		when(oauthRepository.save(any(User.class))).thenReturn(user);
		//then
		User resultUser = oauthService.oauthSignUp(request.getEmail(), request.getSocialType(),
			request.getGender(), request.getAge());

		assertThat(resultUser.getAge()).isEqualTo(request.getAge());
	}

	@Test
	@DisplayName("로그인을 할 수 있다.")
	void signin_test() {

		//given

		//when
		when(oauthRepository.findByEmailAndSocialType(any(String.class),
			any(SocialType.class))).thenReturn(
			Optional.of(user));

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(oauthResponse);

		oauthService.oauthLogin(request);

		//then
		assertThat(oauthResponse.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(oauthResponse.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하지 않으면 회원가입 로직이 실행된다")
	void test_Login_Or_CreateAccount_Based_On_EmailExistence() {

		when(oauthRepository.findByEmailAndSocialType(any(String.class),
			any(SocialType.class))).thenReturn(Optional.empty());

		when(oauthRepository.save(any(User.class))).thenReturn(user);

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(oauthResponse);

		oauthService.oauthLogin(request);

		//then

		// save 메서드가 회원가입시에 1번, 토큰발급 후 1번 -> 총 2번 실행된다
		verify(oauthRepository, times(2)).save(any(User.class));

		assertThat(this.oauthResponse.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(this.oauthResponse.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하면, 회원가입 로직이 실행되지 않는다")
	void test_Login_Or_CreateAccount_Based_On_Email_Not_Existence() {

		when(oauthRepository.findByEmailAndSocialType(any(String.class),
			any(SocialType.class))).thenReturn(Optional.of(user));

		when(oauthRepository.save(any(User.class))).thenReturn(user);

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(oauthResponse);

		oauthService.oauthLogin(request);

		//then

		// save 메서드가 토큰발급 후 1번만 실행된다.
		verify(oauthRepository, times(1)).save(any(User.class));

		assertThat(this.oauthResponse.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(this.oauthResponse.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("토큰 재발급을 할 수 있다.")
	void reissue_token() {

		TokenRequest tokenRequest = new TokenRequest("access", "refresh");

		//when
		when(jwtTokenProvider.validateToken(tokenRequest.refreshToken())).thenReturn(true);
		when(jwtTokenProvider.getAuthentication(tokenRequest.accessToken())).thenReturn(
			mock(Authentication.class));
		when(oauthRepository.findByRefreshToken(tokenRequest.refreshToken())).thenReturn(
			Optional.of(user));
		when(jwtTokenProvider.generateToken(anyString(), any(UserType.class), anyLong()))
			.thenReturn(new OauthResponse("newAccessToken", "newRefreshToken"));

		// then
		OauthResponse response = oauthService.refresh(tokenRequest);

		// 검증
		assertNotNull(response);
		assertThat("newAccessToken").isEqualTo(response.getAccessToken());
		assertThat("newRefreshToken").isEqualTo(response.getRefreshToken());

		verify(jwtTokenProvider).generateToken(user.getEmail(), user.getRole(), user.getId());
	}

	@Test
	@DisplayName("refresh토큰 검증에 실패하면, 토큰 재발급을 할 수 없다.")
	void fail_reissue_token() {

		TokenRequest tokenRequest = new TokenRequest("access", "refresh");

		//when
		when(jwtTokenProvider.validateToken(tokenRequest.refreshToken())).thenReturn(false);

		//토큰 검증에 실패하면 UserException이 발생
		assertThrows(UserException.class, () -> oauthService.refresh(tokenRequest));

		//generateToken 메서드가 실행되지 않음을 검증
		verify(jwtTokenProvider, never()).generateToken(user.getEmail(), user.getRole(),
			user.getId());

	}


}
