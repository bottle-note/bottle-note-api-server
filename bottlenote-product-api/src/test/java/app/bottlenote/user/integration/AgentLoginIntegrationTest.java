package app.bottlenote.user.integration;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static app.bottlenote.agent.constant.AgentStatus.INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import app.bottlenote.agent.support.AgentKeyHasher;
import app.bottlenote.common.constant.AuditPrincipalType;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.AgentLoginRequest;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.fixture.UserTestFactory;
import app.bottlenote.user.repository.OauthRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] POST /api/v2/auth/agent")
class AgentLoginIntegrationTest extends IntegrationTestSupport {

  @Autowired private AgentRepository agentRepository;
  @Autowired private AdminUserRepository adminUserRepository;
  @Autowired private OauthRepository oauthRepository;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("활성 에이전트 키로 매핑된 계정에 로그인하면 기존 OAuth와 동일한 응답과 refresh 쿠키를 내려준다")
  void 활성_에이전트_로그인_성공() throws Exception {
    // given
    String rawKey = apiKey('A');
    User user = saveUser("agent-it-0001");
    saveAgent(rawKey, "9001", user.getId(), true);

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/agent")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AgentLoginRequest(rawKey)))
            .with(csrf())
            .exchange();

    // then
    result.assertThat().hasStatusOk();
    assertThat(result.getResponse().getCookie("refresh-token")).isNotNull();
  }

  @Test
  @DisplayName("API Key 형식이 아닌 에이전트 키는 400을 반환한다")
  void 형식이_잘못된_키는_400() throws Exception {
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/agent")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AgentLoginRequest("invalid-key")))
            .with(csrf())
            .exchange();

    result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("등록되지 않은 에이전트 키는 401을 반환한다")
  void 미등록_키는_401() throws Exception {
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/agent")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AgentLoginRequest(apiKey('B'))))
            .with(csrf())
            .exchange();

    result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("비활성 에이전트 키는 401을 반환한다")
  void 비활성_에이전트는_401() throws Exception {
    // given
    String rawKey = apiKey('C');
    User user = saveUser("agent-it-inactive-agent");
    saveAgent(rawKey, "9002", user.getId(), false);

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/agent")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AgentLoginRequest(rawKey)))
            .with(csrf())
            .exchange();

    // then
    result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("활성 에이전트에 매핑된 Product 계정이 비활성이면 401을 반환한다")
  void 매핑된_Product_계정이_비활성이면_401() throws Exception {
    // given
    String rawKey = apiKey('D');
    User user = saveUser("agent-it-inactive-user");
    user.withdrawUser();
    oauthRepository.save(user);
    saveAgent(rawKey, "9003", user.getId(), true);

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/agent")
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new AgentLoginRequest(rawKey)))
            .with(csrf())
            .exchange();

    // then
    result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("에이전트 로그인으로 발급받은 토큰으로 기존 API를 호출하면 감사 주체가 USER로 기록된다")
  void 에이전트_토큰으로_기존_API를_호출하면_USER로_감사한다() throws Exception {
    // given
    String rawKey = apiKey('E');
    User user = saveUser("agent-it-audit");
    saveAgent(rawKey, "9004", user.getId(), true);

    MvcTestResult loginResult = login(rawKey);
    String accessToken =
        mapper
            .readTree(loginResult.getResponse().getContentAsString())
            .path("accessToken")
            .asText();

    // when
    MvcTestResult result =
        mockMvcTester
            .patch()
            .uri("/api/v1/users/nickname")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(new NicknameChangeRequest("agentAudit")))
            .with(csrf())
            .exchange();

    // then
    result.assertThat().hasStatusOk();
    entityManager.clear();
    User updated = oauthRepository.findById(user.getId()).orElseThrow();
    assertThat(updated.getLastModifyPrincipal().getType()).isEqualTo(AuditPrincipalType.USER);
    assertThat(updated.getLastModifyPrincipal().getId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("에이전트로 재로그인하면 이전 리프레시 토큰은 무효화된다")
  void 에이전트_재로그인은_이전_리프레시_토큰을_무효화한다() throws Exception {
    // given
    String rawKey = apiKey('F');
    User user = saveUser("agent-it-refresh");
    saveAgent(rawKey, "9005", user.getId(), true);

    String firstRefreshToken = login(rawKey).getResponse().getCookie("refresh-token").getValue();
    Thread.sleep(1100);
    login(rawKey).assertThat().hasStatusOk();

    // when
    MvcTestResult result =
        mockMvcTester
            .post()
            .uri("/api/v2/auth/reissue")
            .header("refresh-token", firstRefreshToken)
            .with(csrf())
            .exchange();

    // then
    result.assertThat().hasStatus(HttpStatus.UNAUTHORIZED);
  }

  private Agent saveAgent(String rawKey, String profileCode, Long productUserId, boolean isActive) {
    AdminUser admin = saveAdmin(profileCode);
    return agentRepository.save(
        Agent.builder()
            .id(UUID.randomUUID().toString())
            .profileCode(profileCode)
            .name("BottleNote Agent " + profileCode)
            .status(isActive ? ACTIVE : INACTIVE)
            .productUserId(productUserId)
            .adminUserId(admin.getId())
            .apiKeyHash(AgentKeyHasher.validateAndHash(rawKey))
            .build());
  }

  private User saveUser(String identity) {
    User user =
        oauthRepository.save(
            User.builder()
                .email(identity + "@bottlenote.com")
                .nickName(identity.replace("-", ""))
                .role(UserType.ROLE_USER)
                .socialType(new ArrayList<>(List.of(SocialType.NONE)))
                .build());
    userTestFactory.seedRequiredAgreements(user.getId());
    return user;
  }

  private AdminUser saveAdmin(String profileCode) {
    return adminUserRepository.save(
        AdminUser.builder()
            .email("agent-product-test-" + profileCode + "@bottlenote.com")
            .password("unused-password")
            .name("Agent Product Test " + profileCode)
            .roles(new ArrayList<>(List.of(AdminRole.ROOT_ADMIN)))
            .build());
  }

  private MvcTestResult login(String rawKey) throws Exception {
    return mockMvcTester
        .post()
        .uri("/api/v2/auth/agent")
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsString(new AgentLoginRequest(rawKey)))
        .with(csrf())
        .exchange();
  }

  private static String apiKey(char value) {
    return "bn_agent_" + String.valueOf(value).repeat(43);
  }
}
