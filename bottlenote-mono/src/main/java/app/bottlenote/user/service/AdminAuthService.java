package app.bottlenote.user.service;

import static app.bottlenote.global.security.jwt.JwtTokenValidator.validateToken;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;
import static java.time.LocalDateTime.now;

import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
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

  private final AdminUserRepository adminUserRepository;
  private final JwtTokenProvider tokenProvider;
  private final AdminJwtAuthenticationManager authenticationManager;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public TokenItem login(String email, String encPassword) {
    AdminUser admin =
        adminUserRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (!passwordEncoder.matches(encPassword, admin.getPassword())) {
      throw new UserException(UserExceptionCode.LOGIN_FAILED);
    }

    if (!admin.isActive()) {
      throw new UserException(UserExceptionCode.USER_DELETED);
    }

    TokenItem token =
        tokenProvider.generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId());
    admin.updateRefreshToken(token.refreshToken());
    admin.updateLastLoginAt(now());

    log.info("어드민 로그인: email={}, roles={}", admin.getEmail(), admin.getRoles());
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

    AdminUser admin =
        adminUserRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UserException(INVALID_REFRESH_TOKEN));

    TokenItem newToken =
        tokenProvider.generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId());
    admin.updateRefreshToken(newToken.refreshToken());

    return newToken;
  }

  @Transactional
  public TokenItem signup(Long adminId, String email, String encPassword) {
    // todo: 구현 가능
    // 1. 해당 이메일 사용가능한지 확인.
    // 2. 현재 로그인한 사용자 Id가 Admin User에 있는지 ( 다단계식 회원가입만 가능)
    // 3. 회원 정보 저장
    return null;
  }

  @Transactional
  public void withdraw(Long adminId) {
    AdminUser admin =
        adminUserRepository
            .findById(adminId)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    admin.deactivate();
    log.info("어드민 탈퇴: adminId={}", adminId);
  }

  @Transactional(readOnly = true)
  public boolean rootAdminIsAlive() {
    return adminUserRepository.existsActiveAdmin();
  }

  @Transactional
  public boolean initRootAdmin(String email, String encodedPassword) {
    if (adminUserRepository.existsByEmail(email)) {
      log.info("이미 존재하는 이메일입니다. 초기화를 건너뜁니다. email={}", email);
      return false;
    }

    AdminUser rootAdmin =
        AdminUser.builder()
            .email(email)
            .password(encodedPassword)
            .name("ROOT_ADMIN")
            .roles(List.of(AdminRole.ROOT_ADMIN))
            .build();

    AdminUser saved = adminUserRepository.save(rootAdmin);
    log.info("Root 어드민 계정 생성 완료. adminId={}", saved.getId());
    return true;
  }
}
