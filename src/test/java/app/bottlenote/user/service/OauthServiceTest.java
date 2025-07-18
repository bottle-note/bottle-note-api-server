package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.AppleTokenValidator;
import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.jwt.JwtTokenValidator;
import app.bottlenote.global.service.converter.JsonArrayConverter;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import io.jsonwebtoken.Claims;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[unit] [service] OauthService")
@ExtendWith(MockitoExtension.class)
class OauthServiceTest {

	@InjectMocks
	private OauthService oauthService;
	@Mock
	private JwtAuthenticationManager jwtAuthenticationManager;
	@Mock
	private OauthRepository oauthRepository;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private JsonArrayConverter converter;
	@Mock
	private AppleTokenValidator appleTokenValidator; // 추가
	@Mock
	private NonceService nonceService; // 추가

	private String reissueRefreshToken;
	private OauthRequest request;
	private User user;
	private String nickName = "nickName";

	private TokenItem tokenItem;

	@BeforeEach
	void setUp() {

		request = new OauthRequest("cdm2883@naver.com", null, SocialType.KAKAO,
				null, 26);

		String nickName = "mockNickname";

		user = User.builder()
				.id(1L)
				.email("cdm2883@naver.com")
				.gender(GenderType.MALE)
				.socialType(new ArrayList<>(List.of(SocialType.KAKAO)))
				.age(26)
				.nickName(nickName)
				.role(UserType.ROLE_USER)
				.build();

		tokenItem = TokenItem.builder()
				.accessToken("mock-accessToken")
				.refreshToken("mock-refreshToken")
				.build();

		reissueRefreshToken = "mock-refreshToken";
	}

	@Test
	@DisplayName("로그인을 할 수 있다.")
	void signin_test() {

		//given

		//when
		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
				user.getId())).thenReturn(tokenItem);

		TokenItem result = oauthService.login(request);
		System.out.println(result.accessToken() + " " + result.refreshToken());

		//then
		assertThat(tokenItem.accessToken()).isEqualTo("mock-accessToken");
		assertThat(tokenItem.refreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하지 않으면 회원가입 로직이 실행된다")
	void test_Login_Or_CreateAccount_Based_On_EmailExistence() {

		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		when(oauthRepository.save(any(User.class))).thenReturn(user);

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
				user.getId())).thenReturn(tokenItem);

		oauthService.login(request);

		//then
		verify(oauthRepository, times(1)).save(any(User.class));

		assertThat(this.tokenItem.accessToken()).isEqualTo("mock-accessToken");
		assertThat(this.tokenItem.refreshToken()).isEqualTo("mock-refreshToken");
	}

	@Test
	@DisplayName("로그인 요청 Email이 DB에 존재하면, 회원가입 로직이 실행되지 않는다")
	void test_Login_Or_CreateAccount_Based_On_Email_Not_Existence() {

		//when
		when(oauthRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		when(jwtTokenProvider.generateToken(user.getEmail(), user.getRole(),
				user.getId())).thenReturn(tokenItem);

		oauthService.login(request);

		//then

		// save 메서드가 실행되지 않는다.
		verify(oauthRepository, never()).save(any(User.class));

		assertThat(this.tokenItem.accessToken()).isEqualTo("mock-accessToken");
		assertThat(this.tokenItem.refreshToken()).isEqualTo("mock-refreshToken");
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
					.thenReturn(TokenItem.builder()
							.accessToken("newAccessToken")
							.refreshToken("newRefreshToken")
							.build());

			// then
			TokenItem response = oauthService.refresh(reissueRefreshToken);

			// 검증
			assertNotNull(response);
			assertThat(response.accessToken()).isEqualTo("newAccessToken");
			assertThat(response.refreshToken()).isEqualTo("newRefreshToken");

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

	@Test
	@DisplayName("Apple 로그인 시 기존 사용자를 찾으면 로그인에 성공한다.")
	void loginWithApple_existingUser_success() {
		// Given
		String idToken = "mockIdToken";
		String nonce = "mockNonce";
		String socialUniqueId = "mockSocialUniqueId";
		String email = "test@example.com";

		Claims mockClaims = mock(Claims.class);
		when(appleTokenValidator.validateAndGetClaims(idToken, nonce)).thenReturn(mockClaims);
		when(appleTokenValidator.getAppleSocialUniqueId(mockClaims)).thenReturn(socialUniqueId);
		when(appleTokenValidator.getEmail(mockClaims)).thenReturn(email);

		User existingUser = User.builder()
				.id(2L)
				.email(email)
				.socialUniqueId(socialUniqueId)
				.socialType(List.of(SocialType.APPLE))
				.role(UserType.ROLE_USER)
				.nickName("existingAppleUser")
				.build();
		when(oauthRepository.findBySocialUniqueId(socialUniqueId)).thenReturn(Optional.of(existingUser));
		when(jwtTokenProvider.generateToken(anyString(), any(UserType.class), anyLong())).thenReturn(tokenItem);

		// When
		TokenItem result = oauthService.loginWithApple(idToken, nonce);

		// Then
		assertNotNull(result);
		assertThat(result.accessToken()).isEqualTo(tokenItem.accessToken());
		assertThat(result.refreshToken()).isEqualTo(tokenItem.refreshToken());

		verify(nonceService, times(1)).validateNonce(nonce);
		verify(appleTokenValidator, times(1)).validateAndGetClaims(idToken, nonce);
		verify(oauthRepository, times(1)).findBySocialUniqueId(socialUniqueId);
		verify(oauthRepository, never()).findByEmail(anyString());
		verify(oauthRepository, never()).save(any(User.class));
		verify(jwtTokenProvider, times(1)).generateToken(existingUser.getEmail(), existingUser.getRole(), existingUser.getId());
	}

	@Test
	@DisplayName("Apple 로그인 시 신규 사용자이면 회원가입 후 로그인에 성공한다.")
	void loginWithApple_newUser_success() {
		// Given
		String idToken = "mockIdToken";
		String nonce = "mockNonce";
		String socialUniqueId = "newSocialUniqueId";
		String email = "newuser@example.com";

		Claims mockClaims = mock(Claims.class);
		when(appleTokenValidator.validateAndGetClaims(idToken, nonce)).thenReturn(mockClaims);
		when(appleTokenValidator.getAppleSocialUniqueId(mockClaims)).thenReturn(socialUniqueId);
		when(appleTokenValidator.getEmail(mockClaims)).thenReturn(email);

		when(oauthRepository.findBySocialUniqueId(socialUniqueId)).thenReturn(Optional.empty());
		when(oauthRepository.findByEmail(email)).thenReturn(Optional.empty());

		User newUser = User.builder()
				.id(3L)
				.email(email)
				.socialUniqueId(socialUniqueId)
				.socialType(List.of(SocialType.APPLE))
				.role(UserType.ROLE_USER)
				.nickName("newAppleUser")
				.build();
		when(oauthRepository.save(any(User.class))).thenReturn(newUser);
		when(jwtTokenProvider.generateToken(anyString(), any(UserType.class), anyLong())).thenReturn(tokenItem);

		// When
		TokenItem result = oauthService.loginWithApple(idToken, nonce);

		// Then
		assertNotNull(result);
		assertThat(result.accessToken()).isEqualTo(tokenItem.accessToken());
		assertThat(result.refreshToken()).isEqualTo(tokenItem.refreshToken());

		verify(nonceService, times(1)).validateNonce(nonce);
		verify(appleTokenValidator, times(1)).validateAndGetClaims(idToken, nonce);
		verify(oauthRepository, times(1)).findBySocialUniqueId(socialUniqueId);
		verify(oauthRepository, times(1)).findByEmail(email);
		verify(oauthRepository, times(1)).save(any(User.class));
		verify(jwtTokenProvider, times(1)).generateToken(newUser.getEmail(), newUser.getRole(), newUser.getId());
	}

	@Test
	@DisplayName("Apple 로그인 시 Nonce 검증에 실패하면 예외를 발생시킨다.")
	void loginWithApple_invalidNonce_throwsException() {
		// Given
		String idToken = "mockIdToken";
		String invalidNonce = "invalidNonce";

		doThrow(new IllegalArgumentException("유효하지 않은 Nonce 값입니다."))
				.when(nonceService).validateNonce(invalidNonce);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			oauthService.loginWithApple(idToken, invalidNonce);
		});

		assertThat(exception.getMessage()).isEqualTo("유효하지 않은 Nonce 값입니다.");

		verify(nonceService, times(1)).validateNonce(invalidNonce);
		verify(appleTokenValidator, never()).validateAndGetClaims(anyString(), anyString());
		verify(oauthRepository, never()).findBySocialUniqueId(anyString());
		verify(oauthRepository, never()).findByEmail(anyString());
		verify(oauthRepository, never()).save(any(User.class));
		verify(jwtTokenProvider, never()).generateToken(anyString(), any(UserType.class), anyLong());
	}
}
