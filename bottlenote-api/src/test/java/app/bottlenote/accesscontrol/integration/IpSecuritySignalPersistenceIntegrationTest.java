package app.bottlenote.accesscontrol.integration;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReportRequest;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalResponse;
import app.bottlenote.accesscontrol.exception.IpBanException;
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode;
import app.bottlenote.accesscontrol.service.IpBanService;
import app.bottlenote.accesscontrol.service.IpSecuritySignalService;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.fixture.AdminUserTestFactory;
import app.bottlenote.user.fixture.UserTestFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] IP 보안 signal 영속화")
class IpSecuritySignalPersistenceIntegrationTest extends IntegrationTestSupport {

  @Autowired private IpSecuritySignalService signalService;
  @Autowired private IpBanService ipBanService;
  @Autowired private IpSecuritySignalRepository signalRepository;
  @Autowired private AgentRepository agentRepository;
  @Autowired private AdminUserTestFactory adminUserTestFactory;
  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("밴에 연결한 signal은 관리자·관찰 근거와 UNKNOWN 상태로 저장한다")
  void report_whenLinkedToBan_persistsSignal() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    IpBanDetailResponse ban =
        ipBanService.ban("198.51.100.50", Duration.ofMinutes(10), "abuse", admin.getId());

    IpSecuritySignalResponse signal =
        signalService.report(report(ban.id(), "198.51.100.50", null), admin.getId());

    assertThat(signalRepository.findById(signal.id())).isPresent();
    assertThat(signal.ipBanId()).isEqualTo(ban.id());
    assertThat(signal.verdict()).isEqualTo(SignalVerdict.UNKNOWN);
    assertThat(signal.reportedByAdminUserId()).isEqualTo(admin.getId());
  }

  @Test
  @DisplayName("Agent 매핑 reporter와 버전, false positive 판정을 DB에 저장한다")
  void reportAndReview_whenAgent_persistsMetadataAndVerdict() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    var user = userTestFactory.persistUser();
    String agentId = UUID.randomUUID().toString();
    agentRepository.save(
        Agent.builder()
            .id(agentId)
            .profileCode("0003")
            .name("Security Agent")
            .status(ACTIVE)
            .productUserId(user.getId())
            .adminUserId(admin.getId())
            .apiKeyHash("c".repeat(64))
            .build());

    IpSecuritySignalResponse reported =
        signalService.report(report(null, "2001:DB8::50", "2.4.0"), admin.getId());
    IpSecuritySignalResponse reviewed =
        signalService.review(
            reported.id(), SignalVerdict.FALSE_POSITIVE, "verified", admin.getId());

    assertThat(reviewed.reportedByAgentId()).isEqualTo(agentId);
    assertThat(reviewed.agentVersion()).isEqualTo("2.4.0");
    assertThat(reviewed.verdict()).isEqualTo(SignalVerdict.FALSE_POSITIVE);
    assertThat(reviewed.reviewedByAdminUserId()).isEqualTo(admin.getId());
    assertThat(signalService.list("2001:db8::50", 10)).hasSize(1);
  }

  @Test
  @DisplayName("존재하지 않는 부모 밴을 지정한 signal은 저장하지 않는다")
  void report_whenParentBanMissing_throwsNotFound() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();

    assertThatThrownBy(
            () -> signalService.report(report(999_999L, "198.51.100.51", null), admin.getId()))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.IP_BAN_NOT_FOUND);
  }

  @Test
  @DisplayName("부모 밴과 다른 IP를 지정한 signal은 저장하지 않는다")
  void report_whenParentBanIpDiffers_throwsInvalidSignal() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    IpBanDetailResponse ban =
        ipBanService.ban("198.51.100.52", Duration.ofMinutes(10), "abuse", admin.getId());

    assertThatThrownBy(
            () -> signalService.report(report(ban.id(), "198.51.100.53", null), admin.getId()))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
  }

  @Test
  @DisplayName("동일 signal을 동시에 판정하면 하나의 확정 판정만 저장한다")
  void review_whenSameSignalConcurrent_acceptsOnlyOneVerdict() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    IpSecuritySignalResponse reported =
        signalService.report(report(null, "198.51.100.54", null), admin.getId());
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<String> first =
          executor.submit(
              () ->
                  reviewAfterStart(
                      reported.id(), SignalVerdict.CONFIRMED_ATTACK, admin.getId(), ready, start));
      Future<String> second =
          executor.submit(
              () ->
                  reviewAfterStart(
                      reported.id(), SignalVerdict.FALSE_POSITIVE, admin.getId(), ready, start));

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<String> outcomes =
          List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

      assertThat(outcomes)
          .contains(IpBanExceptionCode.IP_SECURITY_SIGNAL_ALREADY_REVIEWED.name())
          .anyMatch(
              outcome ->
                  outcome.equals(SignalVerdict.CONFIRMED_ATTACK.name())
                      || outcome.equals(SignalVerdict.FALSE_POSITIVE.name()));
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  private String reviewAfterStart(
      Long signalId,
      SignalVerdict verdict,
      Long adminId,
      CountDownLatch ready,
      CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 시작 신호를 받지 못했습니다.");
      }
      return signalService.review(signalId, verdict, "concurrent", adminId).verdict().name();
    } catch (IpBanException exception) {
      return exception.getExceptionCode().toString();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private static IpSecuritySignalReportRequest report(Long banId, String ip, String agentVersion) {
    LocalDateTime observedFrom = LocalDateTime.of(2026, 8, 9, 10, 0);
    return new IpSecuritySignalReportRequest(
        banId,
        ip,
        "/api/v2/auth/login",
        "POST",
        "AUTH_BRUTE_FORCE",
        observedFrom,
        observedFrom.plusMinutes(1),
        10,
        agentVersion);
  }
}
