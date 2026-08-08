package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import app.bottlenote.accesscontrol.dto.request.IpSecuritySignalReport;
import app.bottlenote.accesscontrol.dto.response.IpSecuritySignalView;
import app.bottlenote.accesscontrol.exception.IpBanException;
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode;
import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.global.security.accesscontrol.ClientIpResolver;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요청 원문 없이 IP 보안 signal과 판정을 DB에 보관한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpSecuritySignalService {

  private final IpSecuritySignalRepository signalRepository;
  private final AgentFacade agentFacade;
  private final Clock clock;

  @Transactional
  public IpSecuritySignalView report(IpSecuritySignalReport report, Long reporterAdminUserId) {
    Objects.requireNonNull(report, "report는 null일 수 없습니다.");
    String normalizedIp = requireNormalizedIp(report.rawIp());
    Reporter reporter = resolveReporter(reporterAdminUserId, report.agentVersion());
    try {
      IpSecuritySignal signal =
          signalRepository.save(
              IpSecuritySignal.report(
                  report.ipBanId(),
                  normalizedIp,
                  report.endpointPath(),
                  report.httpMethod(),
                  report.ruleCode(),
                  truncate(report.observedFrom()),
                  truncate(report.observedUntil()),
                  report.observationCount(),
                  reporter.adminUserId(),
                  reporter.agentId(),
                  reporter.agentVersion()));
      return toView(signal);
    } catch (IllegalArgumentException exception) {
      throw new IpBanException(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
    }
  }

  @Transactional
  public IpSecuritySignalView review(
      Long signalId, SignalVerdict verdict, String reviewNote, Long reviewerAdminUserId) {
    if (signalId == null) {
      throw new IpBanException(IpBanExceptionCode.IP_SECURITY_SIGNAL_NOT_FOUND);
    }
    IpSecuritySignal signal =
        signalRepository
            .findById(signalId)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_SECURITY_SIGNAL_NOT_FOUND));
    if (signal.getVerdict() != SignalVerdict.UNKNOWN) {
      throw new IpBanException(IpBanExceptionCode.IP_SECURITY_SIGNAL_ALREADY_REVIEWED);
    }
    try {
      signal.review(verdict, reviewerAdminUserId, now(), reviewNote);
      return toView(signalRepository.save(signal));
    } catch (IllegalArgumentException exception) {
      throw new IpBanException(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
    }
  }

  public Optional<IpSecuritySignalView> getDetail(Long signalId) {
    if (signalId == null) {
      return Optional.empty();
    }
    return signalRepository.findById(signalId).map(this::toView);
  }

  public List<IpSecuritySignalView> list(String rawIp, int max) {
    String normalizedIp = requireNormalizedIp(rawIp);
    return signalRepository
        .findByNormalizedIpOrderByIdDesc(normalizedIp, normalizeLimit(max))
        .stream()
        .map(this::toView)
        .toList();
  }

  private Reporter resolveReporter(Long adminUserId, String agentVersion) {
    if (adminUserId == null) {
      if (agentVersion != null && !agentVersion.isBlank()) {
        throw new IpBanException(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
      }
      return new Reporter(null, null, null);
    }
    return agentFacade
        .findActiveAgentByAdminUserId(adminUserId)
        .map(
            agent -> {
              if (agentVersion == null || agentVersion.isBlank()) {
                throw new IpBanException(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
              }
              return new Reporter(adminUserId, agent.agentId(), agentVersion.trim());
            })
        .orElseGet(
            () -> {
              if (agentVersion != null && !agentVersion.isBlank()) {
                throw new IpBanException(IpBanExceptionCode.INVALID_SECURITY_SIGNAL);
              }
              return new Reporter(adminUserId, null, null);
            });
  }

  private IpSecuritySignalView toView(IpSecuritySignal signal) {
    return new IpSecuritySignalView(
        signal.getId(),
        signal.getIpBanId(),
        signal.getNormalizedIp(),
        signal.getEndpointPath(),
        signal.getHttpMethod(),
        signal.getRuleCode(),
        signal.getObservedFrom(),
        signal.getObservedUntil(),
        signal.getObservationCount(),
        signal.getReportedByAdminUserId(),
        signal.getReportedByAgentId(),
        signal.getAgentVersion(),
        signal.getVerdict(),
        signal.getReviewedByAdminUserId(),
        signal.getReviewedAt(),
        signal.getReviewNote());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
  }

  private static LocalDateTime truncate(LocalDateTime value) {
    if (value == null) {
      throw new IllegalArgumentException("관찰 시각은 null일 수 없습니다.");
    }
    return value.truncatedTo(ChronoUnit.MICROS);
  }

  private static String requireNormalizedIp(String rawIp) {
    String normalized = ClientIpResolver.normalize(rawIp);
    if (normalized == null) {
      throw new IpBanException(IpBanExceptionCode.INVALID_IP);
    }
    return normalized;
  }

  private static int normalizeLimit(int max) {
    return Math.min(Math.max(max, 1), 500);
  }

  private record Reporter(Long adminUserId, String agentId, String agentVersion) {}
}
