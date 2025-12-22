package app.bottlenote.user.service;

import static app.bottlenote.global.security.jwt.JwtTokenValidator.validateToken;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;

import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

  private final OauthRepository oauthRepository;
  private final JwtTokenProvider tokenProvider;
  private final JwtAuthenticationManager authenticationManager;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public TokenItem login(String email, String password) {
    User user =
        oauthRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new UserException(UserExceptionCode.INVALID_PASSWORD);
    }

    if (!user.isAlive()) {
      throw new UserException(UserExceptionCode.USER_DELETED);
    }

    if (user.getRole() != UserType.ROLE_ADMIN) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }

    TokenItem token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(token.refreshToken());

    log.info("어드민 로그인 성공: email={}, userId={}", user.getEmail(), user.getId());

    return token;
  }

  @Transactional
  public TokenItem signUp(Long loginUserId, String email, String password) {
    // 현재 로그인한 사용자가 어드민인지 확인
    User loginUser =
        oauthRepository
            .findById(loginUserId)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (loginUser.getRole() != UserType.ROLE_ADMIN) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }

    // 이메일 중복 검사
    oauthRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);
            });

    String encodedPassword = passwordEncoder.encode(password);
    User newAdmin =
        oauthRepository.save(
            User.builder()
                .email(email)
                .password(encodedPassword)
                .role(UserType.ROLE_ADMIN)
                .socialType(List.of(SocialType.BASIC))
                .nickName(email.split("@")[0])
                .age(0)
                .gender(GenderType.NONE)
                .build());

    TokenItem token =
        tokenProvider.generateToken(newAdmin.getEmail(), newAdmin.getRole(), newAdmin.getId());
    newAdmin.updateRefreshToken(token.refreshToken());

    log.info(
        "어드민 계정 생성: email={}, userId={}, by={}",
        newAdmin.getEmail(),
        newAdmin.getId(),
        loginUserId);

    return token;
  }

  @Transactional
  public TokenItem refresh(String refreshToken) {
    try {
      if (!validateToken(refreshToken)) {
        throw new UserException(INVALID_REFRESH_TOKEN);
      }
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      throw new UserException(INVALID_REFRESH_TOKEN);
    }

    authenticationManager.getAuthentication(refreshToken);

    User user =
        oauthRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UserException(INVALID_REFRESH_TOKEN));

    if (user.getRole() != UserType.ROLE_ADMIN) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }

    TokenItem newToken = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
    user.updateRefreshToken(newToken.refreshToken());

    return newToken;
  }

  @Transactional
  public void withdraw(Long userId) {
    User user =
        oauthRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (user.getRole() != UserType.ROLE_ADMIN) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }

    user.withdrawUser();

    log.info("어드민 탈퇴: userId={}", userId);
  }
}
