package app.bottlenote.user.service;

import static app.bottlenote.global.security.jwt.JwtTokenValidator.validateToken;
import static app.bottlenote.user.exception.UserExceptionCode.AGENT_AUTHENTICATION_FAILED;
import static app.bottlenote.user.exception.UserExceptionCode.AGENT_KEY_INVALID_FORMAT;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;

import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.agent.facade.payload.AgentAccountInfo;
import app.bottlenote.agreement.facade.AgreementFacade;
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
  private final AgreementFacade agreementFacade;
  private final AgentFacade agentFacade;
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

  /**
   * 에이전트 키로 매핑된 계정을 조회해 기존 OAuth와 동일한 토큰을 발급한다. 잘못된 형식, 미등록·비활성 에이전트, 매핑 누락, 비활성 계정을 구분하지 않고 동일한
   * 401로 응답해 계정 존재 여부를 노출하지 않는다.
   */
  @Transactional
  public AuthResponse loginWithAgent(String rawAgentKey) {
    AgentAccountInfo agentAccount;
    try {
      agentAccount =
          agentFacade
              .findActiveAgentAccount(rawAgentKey)
              .orElseThrow(() -> new UserException(AGENT_AUTHENTICATION_FAILED));
    } catch (IllegalArgumentException e) {
      throw new UserException(AGENT_KEY_INVALID_FORMAT);
    }

    User user =
        oauthRepository
            .findById(agentAccount.productUserId())
            .filter(User::isAlive)
            .orElseThrow(() -> new UserException(AGENT_AUTHENTICATION_FAILED));

    return issueAuthResponse(user);
  }

  /** 리프레시 토큰으로 액세스/리프레시 토큰을 재발급한다. */
  @Transactional
  public TokenItem reissue(String refreshToken) {
    try {
      if (!validateToken(refreshToken)) {
        throw new UserException(INVALID_REFRESH_TOKEN);
      }
    } catch (Exception e) {
      throw new UserException(INVALID_REFRESH_TOKEN);
    }

    User user =
        oauthRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UserException(INVALID_REFRESH_TOKEN));

    TokenItem reissuedToken =
        tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(reissuedToken.refreshToken());
    return reissuedToken;
  }

  /** 토큰의 유효성을 검사해 결과 메시지를 반환한다. */
  @Transactional(readOnly = true)
  public String verifyToken(String token) {
    try {
      return validateToken(token) ? "Token is valid" : "Token is invalid {empty}";
    } catch (Exception e) {
      log.error("Token is invalid : {}", e.getMessage());
      return String.format("Token is invalid {%s}", e.getMessage());
    }
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
    return issueAuthResponse(user);
  }

  private AuthResponse issueAuthResponse(User user) {
    TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(token.refreshToken());
    boolean isFirstLogin = user.isFirstLogin();
    user.updateLastLoginAt(java.time.LocalDateTime.now());
    boolean agreementRequired = !agreementFacade.isEligible(user.getId());
    return new AuthResponse(token, isFirstLogin, user.getNickName(), agreementRequired);
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
    List<String> adjectives =
        List.of(
            "부드러운", "향기로운", "숙성된", "풍부한", "깊은", "황금빛", "오크향의", "스모키한", "달콤한", "강렬한",
            "은은한", "묵직한", "섬세한", "산뜻한", "우아한", "따뜻한", "몰티한", "진한", "화사한", "짙은향의",
            "오일리한", "깔끔한", "왁시한", "향긋한", "달큰한", "드라이한", "클래식한", "매력적인", "감미로운", "쌉싸름한",
            "상쾌한", "담백한", "다채로운", "균형잡힌", "개성있는", "펑키한", "낭만적인", "뚜따한", "플로럴한", "싱그러운",
            "포근한", "짙은", "경쾌한", "생기있는", "높은도수의", "기분좋은", "향연가득한", "황이느껴지는", "훈제향의", "여운있는");
    List<String> whiskyTerms =
        List.of(
            "몰트", "버번", "위스키", "바텐더", "오크통", "싱글몰트", "블렌디드", "아이리시", "스카치", "캐스크",
            "보틀킬", "피트", "셰리", "하이볼", "니트", "배럴", "오크", "스피릿", "테이스팅", "글렌캐런",
            "증류기", "시음회", "피니시", "몰트바", "BYOB");
    List<String> brands =
        List.of(
            "글렌피딕", "맥캘란", "라가불린", "탈리스커", "조니워커", "제임슨", "야마자키", "부카나스", "불릿", "잭다니엘스",
            "아드벡", "라프로익", "보모어", "발베니", "글렌리벳", "글렌모렌지", "하이랜드파크", "스프링뱅크", "아란", "벤로막",
            "와일드터키", "메이커스마크", "우드포드리저브", "스태그", "부커스");
    String nickname = adjectives.get(randomValue.nextInt(adjectives.size()));
    if (randomValue.nextInt() % 2 == 0) {
      nickname += whiskyTerms.get(randomValue.nextInt(whiskyTerms.size()));
    } else {
      nickname += brands.get(randomValue.nextInt(brands.size()));
    }
    return nickname + oauthRepository.getNextNicknameSequence();
  }
}
