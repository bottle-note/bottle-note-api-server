package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanAuditRecord;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanEventResponse;
import app.bottlenote.accesscontrol.dto.response.IpBanSummaryResponse;
import app.bottlenote.accesscontrol.exception.IpBanException;
import app.bottlenote.accesscontrol.exception.IpBanExceptionCode;
import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.global.security.accesscontrol.ClientIpResolver;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IP 차단 현재 상태와 감사 이벤트를 한 트랜잭션으로 관리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IpBanService {

  static final Duration MIN_TTL = Duration.ofSeconds(1);
  static final Duration MAX_TTL = Duration.ofDays(30);

  private final IpBanRepository ipBanRepository;
  private final IpBanEventRepository ipBanEventRepository;
  private final AgentFacade agentFacade;
  private final Clock clock;

  @Transactional
  public IpBanDetailResponse ban(String rawIp, Duration ttl, String reason, Long adminUserId) {
    String normalizedIp = requireNormalizedIp(rawIp);
    String sanitizedReason = requireReason(reason);
    Duration validatedTtl = requireTtl(ttl);
    LocalDateTime now = now();
    LocalDateTime expiresAt = now.plus(validatedTtl);
    Actor actor = resolveActor(adminUserId);

    Optional<IpBan> existing = ipBanRepository.findByNormalizedIp(normalizedIp);
    if (existing.isEmpty()) {
      IpBan created =
          ipBanRepository.save(
              IpBan.createActive(normalizedIp, sanitizedReason, now, expiresAt, now));
      appendEvent(
          new EventCommand(
              created.getId(), IpBanEventType.BAN, sanitizedReason, null, expiresAt, actor, now));
      return toDetail(created);
    }

    IpBan ban =
        ipBanRepository
            .findByNormalizedIpForUpdate(normalizedIp)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND));
    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    if (ban.isActive()) {
      ban.extend(sanitizedReason, now, expiresAt);
      ipBanRepository.save(ban);
      appendEvent(
          new EventCommand(
              ban.getId(),
              IpBanEventType.EXTEND,
              sanitizedReason,
              previousExpiresAt,
              expiresAt,
              actor,
              now));
      return toDetail(ban);
    }

    ban.ban(sanitizedReason, now, expiresAt);
    ipBanRepository.save(ban);
    appendEvent(
        new EventCommand(
            ban.getId(),
            IpBanEventType.BAN,
            sanitizedReason,
            previousExpiresAt,
            expiresAt,
            actor,
            now));
    return toDetail(ban);
  }

  @Transactional
  public IpBanDetailResponse unban(String rawIp, String reason, Long adminUserId) {
    String normalizedIp = requireNormalizedIp(rawIp);
    String sanitizedReason = requireReason(reason);
    LocalDateTime now = now();
    Actor actor = resolveActor(adminUserId);

    IpBan ban =
        ipBanRepository
            .findByNormalizedIpForUpdate(normalizedIp)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND));
    if (!ban.isActive()) {
      throw new IpBanException(IpBanExceptionCode.IP_BAN_NOT_ACTIVE);
    }

    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    ban.unban(sanitizedReason, now);
    ipBanRepository.save(ban);
    appendEvent(
        new EventCommand(
            ban.getId(),
            IpBanEventType.UNBAN,
            sanitizedReason,
            previousExpiresAt,
            ban.getExpiresAt(),
            actor,
            now));
    return toDetail(ban);
  }

  @Transactional
  public IpBanDetailResponse expire(String rawIp, String reason) {
    String normalizedIp = requireNormalizedIp(rawIp);
    String sanitizedReason = requireReason(reason);
    LocalDateTime now = now();
    Actor actor = Actor.system();

    IpBan ban =
        ipBanRepository
            .findByNormalizedIpForUpdate(normalizedIp)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND));
    if (!ban.isActive()) {
      throw new IpBanException(IpBanExceptionCode.IP_BAN_NOT_ACTIVE);
    }

    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    ban.expire(sanitizedReason, now);
    ipBanRepository.save(ban);
    appendEvent(
        new EventCommand(
            ban.getId(),
            IpBanEventType.EXPIRE,
            sanitizedReason,
            previousExpiresAt,
            ban.getExpiresAt(),
            actor,
            now));
    return toDetail(ban);
  }

  @Transactional(readOnly = true)
  public Optional<IpBanDetailResponse> getDetail(String rawIp) {
    String normalizedIp = requireNormalizedIp(rawIp);
    return ipBanRepository.findByNormalizedIp(normalizedIp).map(this::toDetail);
  }

  @Transactional(readOnly = true)
  public Optional<IpBanDetailResponse> getDetail(Long id) {
    Objects.requireNonNull(id, "id는 null일 수 없습니다.");
    return ipBanRepository.findById(id).map(this::toDetail);
  }

  @Transactional(readOnly = true)
  public List<IpBanSummaryResponse> list(IpBanStatus status, int max) {
    int limit = normalizeLimit(max);
    List<IpBan> bans =
        status == null
            ? ipBanRepository.findAllOrderByStateChangedAtDesc(limit)
            : ipBanRepository.findByStatusOrderByStateChangedAtDesc(status, limit);
    return bans.stream().map(this::toSummary).toList();
  }

  private void appendEvent(EventCommand command) {
    ipBanEventRepository.save(
        IpBanAuditRecord.create(
            command.ipBanId(),
            command.eventType(),
            command.reason(),
            command.previousExpiresAt(),
            command.nextExpiresAt(),
            command.actor().type(),
            command.actor().adminUserId(),
            command.actor().agentId(),
            command.occurredAt()));
  }

  private Actor resolveActor(Long adminUserId) {
    if (adminUserId == null) {
      return Actor.system();
    }
    return agentFacade
        .findActiveAgentByAdminUserId(adminUserId)
        .map(info -> Actor.agent(adminUserId, info.agentId()))
        .orElse(Actor.admin(adminUserId));
  }

  private IpBanDetailResponse toDetail(IpBan ban) {
    List<IpBanEventResponse> events =
        ipBanEventRepository.findByIpBanIdOrderByIdAsc(ban.getId()).stream()
            .map(this::toEventView)
            .toList();
    return new IpBanDetailResponse(
        ban.getId(),
        ban.getNormalizedIp(),
        ban.getStatus(),
        ban.getReason(),
        ban.getEffectiveFrom(),
        ban.getExpiresAt(),
        ban.getStateChangedAt(),
        events);
  }

  private IpBanSummaryResponse toSummary(IpBan ban) {
    return new IpBanSummaryResponse(
        ban.getId(),
        ban.getNormalizedIp(),
        ban.getStatus(),
        ban.getReason(),
        ban.getEffectiveFrom(),
        ban.getExpiresAt(),
        ban.getStateChangedAt());
  }

  private IpBanEventResponse toEventView(IpBanAuditRecord event) {
    return new IpBanEventResponse(
        event.getId(),
        event.getIpBanId(),
        event.getEventType(),
        event.getReason(),
        event.getPreviousExpiresAt(),
        event.getNextExpiresAt(),
        event.getActorType(),
        event.getActorAdminUserId(),
        event.getActorAgentId(),
        event.getOccurredAt());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
  }

  private static String requireNormalizedIp(String rawIp) {
    String normalized = ClientIpResolver.normalize(rawIp);
    if (normalized == null) {
      throw new IpBanException(IpBanExceptionCode.INVALID_IP);
    }
    return normalized;
  }

  private static String requireReason(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IpBanException(IpBanExceptionCode.INVALID_REASON);
    }
    String trimmed = reason.trim();
    if (trimmed.length() > 200) {
      throw new IpBanException(IpBanExceptionCode.INVALID_REASON);
    }
    return trimmed;
  }

  private static Duration requireTtl(Duration ttl) {
    if (ttl == null || ttl.compareTo(MIN_TTL) < 0 || ttl.compareTo(MAX_TTL) > 0) {
      throw new IpBanException(IpBanExceptionCode.INVALID_TTL);
    }
    return ttl;
  }

  private static int normalizeLimit(int max) {
    if (max < 1) {
      return 1;
    }
    return Math.min(max, 500);
  }

  private record Actor(IpBanActorType type, Long adminUserId, String agentId) {
    static Actor system() {
      return new Actor(IpBanActorType.SYSTEM, null, null);
    }

    static Actor admin(Long adminUserId) {
      return new Actor(IpBanActorType.ADMIN, adminUserId, null);
    }

    static Actor agent(Long adminUserId, String agentId) {
      return new Actor(IpBanActorType.AGENT, adminUserId, agentId);
    }
  }

  private record EventCommand(
      Long ipBanId,
      IpBanEventType eventType,
      String reason,
      LocalDateTime previousExpiresAt,
      LocalDateTime nextExpiresAt,
      Actor actor,
      LocalDateTime occurredAt) {}
}
