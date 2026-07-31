package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import app.bottlenote.agent.support.AgentKeyHasher;
import app.bottlenote.global.security.CustomAdminUserDetailsService;
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fixture.InMemoryAdminUserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Tag("unit")
@DisplayName("AdminAuthService 단위 테스트")
class AdminAuthServiceTest {

  private static final String TEST_JWT_SECRET =
      "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10ZXN0aW5nLXB1cnBvc2UtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTUxMi12ZXJzaW9uLWFuZC1pbXBvcnRhbnQtc2VjdXJpdHktaW4tbW9kZXJuLWphdmEtYXBwbGljYXRpb25z";

  InMemoryAdminUserRepository adminUserRepository;
  InMemoryAgentRepository agentRepository;
  AgentFacade agentFacade;
  AdminAuthService adminAuthService;

  @BeforeEach
  void setUp() {
    adminUserRepository = new InMemoryAdminUserRepository();
    agentRepository = new InMemoryAgentRepository();
    agentFacade = new DefaultAgentFacade(agentRepository);

    adminAuthService =
        new AdminAuthService(
            adminUserRepository,
            new JwtTokenProvider(TEST_JWT_SECRET),
            new AdminJwtAuthenticationManager(
                TEST_JWT_SECRET, new CustomAdminUserDetailsService(adminUserRepository)),
            new BCryptPasswordEncoder(),
            agentFacade);
  }

  private AdminUser saveActiveAdmin(String agentId) {
    AdminUser admin =
        AdminUser.builder()
            .email("agent-admin@bottlenote.com")
            .password("encoded")
            .name("Agent Admin")
            .roles(List.of(AdminRole.ROOT_ADMIN))
            .status(UserStatus.ACTIVE)
            .agentId(agentId)
            .build();
    return adminUserRepository.save(admin);
  }

  @Test
  @DisplayName("활성 에이전트 키로 매핑된 관리자 계정으로 로그인할 수 있다")
  void loginWithAgent_success() {
    // given
    String rawKey = "11111111-1111-1111-1111-111111111111";
    String agentId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    agentRepository.save(
        Agent.builder()
            .id(agentId)
            .profileCode("0001")
            .secretHash(AgentKeyHasher.normalizeAndHash(rawKey))
            .isActive(true)
            .build());
    saveActiveAdmin(agentId);

    // when
    TokenItem token = adminAuthService.loginWithAgent(rawKey);

    // then
    assertThat(token.accessToken()).isNotNull();
    assertThat(token.refreshToken()).isNotNull();
  }

  @Test
  @DisplayName("에이전트 키가 UUID 형식이 아니면 400에 해당하는 예외가 발생한다")
  void loginWithAgent_malformedKey_throwsInvalidFormat() {
    // when
    UserException exception =
        assertThrows(UserException.class, () -> adminAuthService.loginWithAgent("not-a-uuid"));

    // then
    assertThat(exception.getExceptionCode()).isEqualTo(UserExceptionCode.AGENT_KEY_INVALID_FORMAT);
  }

  @Test
  @DisplayName("등록되지 않은 에이전트 키는 401에 해당하는 예외가 발생한다")
  void loginWithAgent_unknownKey_throwsAuthenticationFailed() {
    // given
    String unknownKey = "22222222-2222-2222-2222-222222222222";

    // when
    UserException exception =
        assertThrows(UserException.class, () -> adminAuthService.loginWithAgent(unknownKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("비활성 에이전트 키는 401에 해당하는 예외가 발생한다")
  void loginWithAgent_inactiveAgent_throwsAuthenticationFailed() {
    // given
    String rawKey = "33333333-3333-3333-3333-333333333333";
    agentRepository.save(
        Agent.builder()
            .id("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
            .profileCode("0002")
            .secretHash(AgentKeyHasher.normalizeAndHash(rawKey))
            .isActive(false)
            .build());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> adminAuthService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("활성 에이전트라도 매핑된 관리자 계정이 없으면 401에 해당하는 예외가 발생한다")
  void loginWithAgent_missingAccountMapping_throwsAuthenticationFailed() {
    // given
    String rawKey = "44444444-4444-4444-4444-444444444444";
    agentRepository.save(
        Agent.builder()
            .id("cccccccc-cccc-cccc-cccc-cccccccccccc")
            .profileCode("0003")
            .secretHash(AgentKeyHasher.normalizeAndHash(rawKey))
            .isActive(true)
            .build());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> adminAuthService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }

  @Test
  @DisplayName("에이전트에 매핑된 관리자 계정이 비활성 상태면 401에 해당하는 예외가 발생한다")
  void loginWithAgent_inactiveAdmin_throwsAuthenticationFailed() {
    // given
    String rawKey = "55555555-5555-5555-5555-555555555555";
    String agentId = "dddddddd-dddd-dddd-dddd-dddddddddddd";
    agentRepository.save(
        Agent.builder()
            .id(agentId)
            .profileCode("0004")
            .secretHash(AgentKeyHasher.normalizeAndHash(rawKey))
            .isActive(true)
            .build());

    AdminUser inactiveAdmin =
        AdminUser.builder()
            .email("inactive-agent-admin@bottlenote.com")
            .password("encoded")
            .name("Inactive Agent Admin")
            .roles(List.of(AdminRole.ROOT_ADMIN))
            .status(UserStatus.DELETED)
            .agentId(agentId)
            .build();
    adminUserRepository.save(inactiveAdmin);

    // when
    UserException exception =
        assertThrows(UserException.class, () -> adminAuthService.loginWithAgent(rawKey));

    // then
    assertThat(exception.getExceptionCode())
        .isEqualTo(UserExceptionCode.AGENT_AUTHENTICATION_FAILED);
  }
}
