package app.bottlenote.user.service;

import static app.bottlenote.global.security.jwt.JwtTokenValidator.validateToken;
import static app.bottlenote.user.exception.UserExceptionCode.AGENT_AUTHENTICATION_FAILED;
import static app.bottlenote.user.exception.UserExceptionCode.AGENT_KEY_INVALID_FORMAT;
import static app.bottlenote.user.exception.UserExceptionCode.INVALID_REFRESH_TOKEN;
import static java.time.LocalDateTime.now;

import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.agent.facade.payload.AgentAccountInfo;
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import app.bottlenote.user.dto.request.AdminSignupRequest;
import app.bottlenote.user.dto.response.AdminSignupResponse;
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
  private final AgentFacade agentFacade;

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

  /**
   * 에이전트 키로 매핑된 관리자 계정을 조회해 기존 로그인과 동일한 토큰을 발급한다. 잘못된 형식, 미등록·비활성 에이전트, 매핑 누락, 비활성 계정을 구분하지 않고 동일한
   * 401로 응답해 계정 존재 여부를 노출하지 않는다.
   */
  @Transactional
  public TokenItem loginWithAgent(String rawAgentKey) {
    AgentAccountInfo agentAccount;
    try {
      agentAccount =
          agentFacade
              .findActiveAgentAccount(rawAgentKey)
              .orElseThrow(() -> new UserException(AGENT_AUTHENTICATION_FAILED));
    } catch (IllegalArgumentException e) {
      throw new UserException(AGENT_KEY_INVALID_FORMAT);
    }

    AdminUser admin =
        adminUserRepository
            .findById(agentAccount.adminUserId())
            .filter(AdminUser::isActive)
            .orElseThrow(() -> new UserException(AGENT_AUTHENTICATION_FAILED));

    TokenItem token =
        tokenProvider.generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId());
    admin.updateRefreshToken(token.refreshToken());
    admin.updateLastLoginAt(now());

    log.info("어드민 에이전트 로그인: adminId={}, roles={}", admin.getId(), admin.getRoles());
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
  public AdminSignupResponse signup(Long requesterId, AdminSignupRequest request) {
    AdminUser requester =
        adminUserRepository
            .findById(requesterId)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (!requester.isActive()) {
      throw new UserException(UserExceptionCode.USER_DELETED);
    }

    validateRoleAssignment(requester, request.roles());

    if (adminUserRepository.existsByEmail(request.email())) {
      throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);
    }

    AdminUser newAdmin =
        AdminUser.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .roles(request.roles())
            .build();

    AdminUser saved = adminUserRepository.save(newAdmin);

    log.info(
        "어드민 계정 생성: adminId={}, email={}, roles={}, 생성자={}",
        saved.getId(),
        saved.getEmail(),
        saved.getRoles(),
        requesterId);

    return new AdminSignupResponse(
        saved.getId(), saved.getEmail(), saved.getName(), saved.getRoles());
  }

  private void validateRoleAssignment(AdminUser requester, List<AdminRole> requestedRoles) {
    if (requestedRoles.contains(AdminRole.ROOT_ADMIN) && !requester.hasRole(AdminRole.ROOT_ADMIN)) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }
  }

  @Transactional
  public void withdraw(Long adminId) {
    AdminUser admin =
        adminUserRepository
            .findById(adminId)
            .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    if (admin.hasRole(AdminRole.ROOT_ADMIN)) {
      throw new UserException(UserExceptionCode.ACCESS_DENIED);
    }

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
