package app.bottlenote.accesscontrol.service;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReportRequest;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalResponse;
import app.bottlenote.accesscontrol.exception.IpBanException;
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpSecuritySignalRepository;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("IpSecuritySignalService 단위 테스트")
class IpSecuritySignalServiceTest {
  private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final Instant NOW = Instant.parse("2026-08-09T03:00:00Z");
  private static final LocalDateTime OBSERVED_FROM = LocalDateTime.of(2026, 8, 9, 11, 59);
  private static final LocalDateTime OBSERVED_UNTIL = LocalDateTime.of(2026, 8, 9, 12, 0);

  private InMemoryIpSecuritySignalRepository signalRepository;
  private InMemoryIpBanRepository banRepository;
  private InMemoryAgentRepository agentRepository;
  private IpSecuritySignalService service;

  @BeforeEach
  void setUp() {
    signalRepository = new InMemoryIpSecuritySignalRepository();
    banRepository = new InMemoryIpBanRepository();
    agentRepository = new InMemoryAgentRepository();
    service =
        new IpSecuritySignalService(
            signalRepository,
            banRepository,
            new DefaultAgentFacade(agentRepository),
            Clock.fixed(NOW, ZONE_ID));
  }

  @Test
  @DisplayName("관리자가 signal을 등록하면 query string 없이 UNKNOWN 상태로 저장한다")
  void report_whenAdmin_persistsUnknownSignal() {
    IpSecuritySignalResponse view = service.report(report("198.51.100.40", null), 11L);

    assertThat(view.normalizedIp()).isEqualTo("198.51.100.40");
    assertThat(view.endpointPath()).isEqualTo("/api/v2/reviews");
    assertThat(view.httpMethod()).isEqualTo("POST");
    assertThat(view.verdict()).isEqualTo(SignalVerdict.UNKNOWN);
    assertThat(view.reportedByAdminUserId()).isEqualTo(11L);
    assertThat(view.reportedByAgentId()).isNull();
    assertThat(view.reviewedAt()).isNull();
  }

  @Test
  @DisplayName("활성 Agent에 매핑된 관리자가 버전과 함께 등록하면 Agent 메타데이터를 저장한다")
  void report_whenActiveAgent_persistsAgentMetadata() {
    String agentId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    agentRepository.save(
        Agent.builder()
            .id(agentId)
            .profileCode("0001")
            .name("Security Agent")
            .status(ACTIVE)
            .productUserId(10L)
            .adminUserId(42L)
            .apiKeyHash("a".repeat(64))
            .build());

    IpSecuritySignalResponse view = service.report(report("2001:DB8::40", "1.2.3"), 42L);

    assertThat(view.normalizedIp()).isEqualTo("2001:db8::40");
    assertThat(view.reportedByAgentId()).isEqualTo(agentId);
    assertThat(view.agentVersion()).isEqualTo("1.2.3");
  }

  @Test
  @DisplayName("UNKNOWN signal은 공격으로 판정하고 검토 정보를 기록한다")
  void review_whenUnknown_recordsConfirmedAttack() {
    IpSecuritySignalResponse reported = service.report(report("198.51.100.41", null), 11L);

    IpSecuritySignalResponse reviewed =
        service.review(reported.id(), SignalVerdict.CONFIRMED_ATTACK, "reproduced", 12L);

    assertThat(reviewed.verdict()).isEqualTo(SignalVerdict.CONFIRMED_ATTACK);
    assertThat(reviewed.reviewedByAdminUserId()).isEqualTo(12L);
    assertThat(reviewed.reviewedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZONE_ID));
    assertThat(reviewed.reviewNote()).isEqualTo("reproduced");
  }

  @Test
  @DisplayName("이미 판정한 signal은 다시 판정할 수 없다")
  void review_whenAlreadyReviewed_throws() {
    IpSecuritySignalResponse reported = service.report(report("198.51.100.42", null), 11L);
    service.review(reported.id(), SignalVerdict.FALSE_POSITIVE, "safe", 12L);

    assertThatThrownBy(
            () -> service.review(reported.id(), SignalVerdict.CONFIRMED_ATTACK, "again", 12L))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.IP_SECURITY_SIGNAL_ALREADY_REVIEWED);
  }

  @Test
  @DisplayName("query string이 포함된 endpoint는 저장하지 않는다")
  void report_whenEndpointHasQueryString_throws() {
    IpSecuritySignalReportRequest invalid =
        new IpSecuritySignalReportRequest(
            null,
            "198.51.100.43",
            "/api/v2/reviews?token=secret",
            "POST",
            "RATE_LIMIT",
            OBSERVED_FROM,
            OBSERVED_UNTIL,
            3,
            null);

    assertThatThrownBy(() -> service.report(invalid, 11L))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
  }

  @Test
  @DisplayName("활성 Agent signal은 agentVersion 없이는 저장하지 않는다")
  void report_whenAgentVersionMissing_throws() {
    agentRepository.save(
        Agent.builder()
            .id("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
            .profileCode("0002")
            .name("Security Agent")
            .status(ACTIVE)
            .productUserId(20L)
            .adminUserId(43L)
            .apiKeyHash("b".repeat(64))
            .build());

    assertThatThrownBy(() -> service.report(report("198.51.100.44", null), 43L))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
  }

  @Test
  @DisplayName("존재하지 않는 부모 밴으로 signal을 등록하면 not found를 반환한다")
  void report_whenParentBanIsMissing_throwsNotFound() {
    assertThatThrownBy(() -> service.report(report(999L, "198.51.100.45", null), 11L))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.IP_BAN_NOT_FOUND);
  }

  @Test
  @DisplayName("부모 밴과 다른 IP로 signal을 등록하면 거부한다")
  void report_whenParentBanIpDiffers_throwsInvalidSignal() {
    IpBan ban =
        banRepository.save(
            IpBan.createActive(
                "198.51.100.46",
                "abuse",
                OBSERVED_FROM,
                OBSERVED_FROM.plus(Duration.ofMinutes(10)),
                OBSERVED_FROM));

    assertThatThrownBy(() -> service.report(report(ban.getId(), "198.51.100.47", null), 11L))
        .isInstanceOf(IpBanException.class)
        .extracting("exceptionCode")
        .isEqualTo(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
  }

  private static IpSecuritySignalReportRequest report(String ip, String agentVersion) {
    return report(null, ip, agentVersion);
  }

  private static IpSecuritySignalReportRequest report(Long banId, String ip, String agentVersion) {
    return new IpSecuritySignalReportRequest(
        banId,
        ip,
        "/api/v2/reviews",
        "post",
        "RATE_LIMIT",
        OBSERVED_FROM,
        OBSERVED_UNTIL,
        3,
        agentVersion);
  }
}
