package app.bottlenote.user.service;

import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.AuthResponse;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import app.bottlenote.user.repository.RootAdminRepository;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final RootAdminRepository rootAdminRepository;
  private final OauthRepository oauthRepository;
  private final JwtTokenProvider tokenProvider;
  private final AppleAuthService appleAuthService;
  private final KakaoAuthService kakaoAuthService;
  private final SecureRandom randomValue = new SecureRandom();

  @Transactional(readOnly = true)
  public boolean checkAdminStatus(Long userId) {
    return rootAdminRepository.existsByUserId(userId);
  }

  @Transactional
  public AuthResponse loginWithApple(String idToken, String nonce) {
    AppleAuthService.AppleUserInfo appleUserInfo =
        appleAuthService.validateAndGetUserInfo(idToken, nonce);

    String socialUniqueId = appleUserInfo.socialUniqueId();
    String email = appleUserInfo.email();

    User user =
        oauthRepository
            .findBySocialUniqueId(socialUniqueId)
            .orElseGet(() -> findByEmailOrCreateAppleUser(email, socialUniqueId));

    checkActiveUser(user);
    return getAuthResult(user, SocialType.APPLE);
  }

  @Transactional
  public AuthResponse loginWithKakao(String accessToken) {
    KakaoUserResponse kakaoUser = kakaoAuthService.getUserInfo(accessToken);

    String kakaoId = String.valueOf(kakaoUser.id());
    KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
    String email = account != null ? account.email() : null;

    log.info("카카오 로그인 시도 - kakaoId: {}, email: {}", kakaoId, email);

    User user =
        oauthRepository
            .findBySocialUniqueId(kakaoId)
            .orElseGet(() -> findByEmailOrCreateKakaoUser(kakaoUser));

    checkActiveUser(user);
    return getAuthResult(user, SocialType.KAKAO);
  }

  private User findByEmailOrCreateAppleUser(String email, String socialUniqueId) {
    return oauthRepository
        .findByEmail(email)
        .map(
            existingUser -> {
              log.info("기존 계정({})에 Apple 계정 연동: socialUniqueId={}", email, socialUniqueId);
              existingUser.updateSocialUniqueId(socialUniqueId);
              return existingUser;
            })
        .orElseGet(
            () -> {
              log.info("Apple 신규 회원가입: email={}, socialUniqueId={}", email, socialUniqueId);
              return signupWithApple(email, socialUniqueId);
            });
  }

  private User signupWithApple(String email, String socialUniqueId) {
    User user =
        User.builder()
            .email(email)
            .socialUniqueId(socialUniqueId)
            .socialType(List.of(SocialType.APPLE))
            .role(UserType.ROLE_USER)
            .nickName(generateNickname())
            .build();
    return oauthRepository.save(user);
  }

  private User findByEmailOrCreateKakaoUser(KakaoUserResponse kakaoUser) {
    String kakaoId = String.valueOf(kakaoUser.id());
    KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
    String email = account != null ? account.email() : null;

    if (email == null || email.isBlank()) {
      log.info("카카오 신규 회원가입 (이메일 없음): kakaoId={}", kakaoId);
      return signupWithKakao(kakaoUser);
    }

    return oauthRepository
        .findByEmail(email)
        .map(
            existingUser -> {
              log.info("기존 계정({})에 카카오 계정 연동: kakaoId={}", email, kakaoId);
              existingUser.updateSocialUniqueId(kakaoId);
              return existingUser;
            })
        .orElseGet(
            () -> {
              log.info("카카오 신규 회원가입: email={}, kakaoId={}", email, kakaoId);
              return signupWithKakao(kakaoUser);
            });
  }

  private User signupWithKakao(KakaoUserResponse kakaoUser) {
    String kakaoId = String.valueOf(kakaoUser.id());
    KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
    String email = account != null ? account.email() : null;
    String userEmail = email != null ? email : "kakao" + UUID.randomUUID() + "@bottlenote.com";

    GenderType gender = extractGenderFromKakao(account);
    Integer age = extractAgeFromKakao(account);

    User user =
        User.builder()
            .email(userEmail)
            .socialUniqueId(kakaoId)
            .socialType(List.of(SocialType.KAKAO))
            .role(UserType.ROLE_USER)
            .gender(gender)
            .age(age)
            .nickName(generateNickname())
            .build();
    return oauthRepository.save(user);
  }

  private void checkActiveUser(User user) {
    if (!user.isAlive()) throw new UserException(UserExceptionCode.USER_DELETED);
  }

  private AuthResponse getAuthResult(User user, SocialType socialType) {
    user.addSocialType(socialType);
    TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(token.refreshToken());
    boolean isFirstLogin = user.isFirstLogin();
    user.updateLastLoginAt(java.time.LocalDateTime.now());
    return new AuthResponse(token, isFirstLogin, user.getNickName());
  }

  private GenderType extractGenderFromKakao(KakaoUserResponse.KakaoAccount account) {
    if (account != null && account.gender() != null) {
      return switch (account.gender().toLowerCase()) {
        case "female" -> GenderType.FEMALE;
        case "male" -> GenderType.MALE;
        default -> null;
      };
    }
    return null;
  }

  private Integer extractAgeFromKakao(KakaoUserResponse.KakaoAccount account) {
    if (account != null && account.ageRange() != null) {
      String ageRange = account.ageRange();
      try {
        if (ageRange.contains("~")) {
          String[] range = ageRange.split("~");
          int min = Integer.parseInt(range[0]);
          int max = Integer.parseInt(range[1]);
          return (min + max) / 2;
        }
      } catch (NumberFormatException e) {
        log.warn("카카오 연령대 파싱 실패: {}", ageRange);
      }
    }
    return null;
  }

  private String generateNickname() {
    List<String> a =
        Arrays.asList("부드러운", "향기로운", "숙성된", "풍부한", "깊은", "황금빛", "오크향의", "스모키한", "달콤한", "강렬한");
    List<String> b =
        Arrays.asList("몰트", "버번", "위스키", "바텐더", "오크통", "싱글몰트", "블렌디드", "아이리시", "스카치", "캐스크");
    List<String> c =
        Arrays.asList("글렌피딕", "맥캘란", "라가불린", "탈리스커", "조니워커", "제임슨", "야마자키", "부카나스", "불릿", "잭다니엘스");
    String key = a.get(randomValue.nextInt(a.size()));
    if (randomValue.nextInt() % 2 == 0) key += b.get(randomValue.nextInt(b.size()));
    else key += c.get(randomValue.nextInt(c.size()));
    return key + oauthRepository.getNextNicknameSequence();
  }
}
