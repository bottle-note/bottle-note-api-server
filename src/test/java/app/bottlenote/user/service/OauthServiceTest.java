package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.global.service.converter.JsonArrayConverter;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenDto;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@Tag("unit")
@DisplayName("[unit] [service] OauthService")
@ExtendWith(MockitoExtension.class)
class OauthServiceTest {

	@InjectMocks
	private OauthService oauthService;
	@Mock
	private JwtAuthenticationManager jwtAuthenticationManager;
	@Mock
	private NicknameGenerator nicknameGenerator;
	@Mock
	private OauthRepository oauthRepository;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private JsonArrayConverter converter;

	private String reissueRefreshToken;
	private OauthRequest request;
	private User user;
	private String nickName = "nickName";

	private TokenDto tokenDto;

	@BeforeEach
	void setUp() {

		request = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO,
			null, 26);

		String nickName = "mockNickname";

		user = User.builder()
			.id(1L)
			.email("cdm2883@naver.com")
			.gender("남")
			.socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
			.age(26)
			.nickName(nickName)
			.role(UserType.ROLE_USER)
			.build();

		tokenDto = TokenDto.builder()
			.accessToken("mock-accessToken")
			.refreshToken("mock-refreshToken")
			.build();

		reissueRefreshToken = "mock-refreshToken";
	}

	@Test
	@DisplayName("회원가입을 할 수 있다.")
	void signup_test() {

		//given

		//when
		when(nicknameGenerator.generateNickname()).thenReturn(nickName);

		when(oauthRepository.save(any(User.class))).thenReturn(user);
		//then
		User resultUser = oauthService.oauthSignUp(request.email(), request.socialType(),
			request.gender(), request.age());

		assertThat(resultUser.getAge()).isEqualTo(request.age());
	}

	@Test
	@DisplayName("로그인을 할 수 있다.")
	void signin_test() {

		//given

		//when
		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(tokenDto);

		TokenDto result = oauthService.oauthLogin(request);
		System.out.println(result.getAccessToken() + " " + result.getRefreshToken());

		//then
		assertThat(tokenDto.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(tokenDto.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하지 않으면 회원가입 로직이 실행된다")
	void test_Login_Or_CreateAccount_Based_On_EmailExistence() {

		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		when(oauthRepository.save(any(User.class))).thenReturn(user);

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(tokenDto);

		oauthService.oauthLogin(request);

		//then
		verify(oauthRepository, times(1)).save(any(User.class));

		assertThat(this.tokenDto.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(this.tokenDto.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하면, 회원가입 로직이 실행되지 않는다")
	void test_Login_Or_CreateAccount_Based_On_Email_Not_Existence() {

		//when
		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
			user.getId())).thenReturn(tokenDto);

		oauthService.oauthLogin(request);

		//then

		// save 메서드가 실행되지 않는다.
		verify(oauthRepository, never()).save(any(User.class));

		assertThat(this.tokenDto.getAccessToken()).isEqualTo("mock-accessToken");
		assertThat(this.tokenDto.getRefreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("토큰 재발급을 할 수 있다.")
	void reissue_token() {
		try (MockedStatic<JwtTokenValidator> mockedValidator = mockStatic(
			JwtTokenValidator.class)) {

			//when
			mockedValidator.when(() -> JwtTokenValidator.validateToken(reissueRefreshToken))
				.thenReturn(true);

			when(jwtAuthenticationManager.getAuthentication(reissueRefreshToken)).thenReturn(
				mock(Authentication.class));

			when(oauthRepository.findByRefreshToken(reissueRefreshToken)).thenReturn(
				Optional.of(user));

			when(jwtTokenProvider.generateToken(anyString(), any(UserType.class), anyLong()))
				.thenReturn(TokenDto.builder()
					.accessToken("newAccessToken")
					.refreshToken("newRefreshToken")
					.build());

			// then
			TokenDto response = oauthService.refresh(reissueRefreshToken);

			// 검증
			assertNotNull(response);
			assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
			assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");

			verify(jwtTokenProvider).generateToken(user.getEmail(), user.getRole(), user.getId());
		}
	}

	@Test
	@DisplayName("토큰 검증에 통과하지 못하면 토큰 재발급에 실패한다")
	void reissue_token_fail() {
		try (MockedStatic<JwtTokenValidator> mockedValidator = mockStatic(
			JwtTokenValidator.class)) {

			//when
			mockedValidator.when(() -> JwtTokenValidator.validateToken(reissueRefreshToken))
				.thenReturn(false);

			// then
			assertThrows(UserException.class, () -> oauthService.refresh(reissueRefreshToken));
		}
	}
}
