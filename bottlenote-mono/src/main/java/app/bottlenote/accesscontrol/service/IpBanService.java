package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanEvent;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.accesscontrol.dto.response.IpBanDetail;
import app.bottlenote.accesscontrol.dto.response.IpBanEventView;
import app.bottlenote.accesscontrol.dto.response.IpBanSummary;
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
  public IpBanDetail ban(String rawIp, Duration ttl, String reason, Long adminUserId) {
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
          created.getId(), IpBanEventType.BAN, sanitizedReason, null, expiresAt, actor, now);
      return toDetail(created);
    }

    IpBan ban = existing.get();
    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    if (ban.isActive()) {
      ban.extend(sanitizedReason, now, expiresAt);
      ipBanRepository.save(ban);
      appendEvent(
          ban.getId(),
          IpBanEventType.EXTEND,
          sanitizedReason,
          previousExpiresAt,
          expiresAt,
          actor,
          now);
      return toDetail(ban);
    }

    ban.ban(sanitizedReason, now, expiresAt);
    ipBanRepository.save(ban);
    appendEvent(
        ban.getId(), IpBanEventType.BAN, sanitizedReason, previousExpiresAt, expiresAt, actor, now);
    return toDetail(ban);
  }

  @Transactional
  public IpBanDetail unban(String rawIp, String reason, Long adminUserId) {
    String normalizedIp = requireNormalizedIp(rawIp);
    String sanitizedReason = requireReason(reason);
    LocalDateTime now = now();
    Actor actor = resolveActor(adminUserId);

    IpBan ban =
        ipBanRepository
            .findByNormalizedIp(normalizedIp)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND));
    if (!ban.isActive()) {
      throw new IpBanException(IpBanExceptionCode.IP_BAN_NOT_ACTIVE);
    }

    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    ban.unban(sanitizedReason, now);
    ipBanRepository.save(ban);
    appendEvent(
        ban.getId(),
        IpBanEventType.UNBAN,
        sanitizedReason,
        previousExpiresAt,
        ban.getExpiresAt(),
        actor,
        now);
    return toDetail(ban);
  }

  @Transactional
  public IpBanDetail expire(String rawIp, String reason) {
    String normalizedIp = requireNormalizedIp(rawIp);
    String sanitizedReason = requireReason(reason);
    LocalDateTime now = now();
    Actor actor = Actor.system();

    IpBan ban =
        ipBanRepository
            .findByNormalizedIp(normalizedIp)
            .orElseThrow(() -> new IpBanException(IpBanExceptionCode.IP_BAN_NOT_FOUND));
    if (!ban.isActive()) {
      throw new IpBanException(IpBanExceptionCode.IP_BAN_NOT_ACTIVE);
    }

    LocalDateTime previousExpiresAt = ban.getExpiresAt();
    ban.expire(sanitizedReason, now);
    ipBanRepository.save(ban);
    appendEvent(
        ban.getId(),
        IpBanEventType.EXPIRE,
        sanitizedReason,
        previousExpiresAt,
        ban.getExpiresAt(),
        actor,
        now);
    return toDetail(ban);
  }

  public Optional<IpBanDetail> getDetail(String rawIp) {
    String normalizedIp = requireNormalizedIp(rawIp);
    return ipBanRepository.findByNormalizedIp(normalizedIp).map(this::toDetail);
  }

  public Optional<IpBanDetail> getDetail(Long id) {
    Objects.requireNonNull(id, "id는 null일 수 없습니다.");
    return ipBanRepository.findById(id).map(this::toDetail);
  }

  public List<IpBanSummary> list(IpBanStatus status, int max) {
    int limit = normalizeLimit(max);
    List<IpBan> bans =
        status == null
            ? ipBanRepository.findAllOrderByStateChangedAtDesc(limit)
            : ipBanRepository.findByStatusOrderByStateChangedAtDesc(status, limit);
    return bans.stream().map(this::toSummary).toList();
  }

  private void appendEvent(
      Long ipBanId,
      IpBanEventType eventType,
      String reason,
      LocalDateTime previousExpiresAt,
      LocalDateTime nextExpiresAt,
      Actor actor,
      LocalDateTime occurredAt) {
    ipBanEventRepository.save(
        IpBanEvent.create(
            ipBanId,
            eventType,
            reason,
            previousExpiresAt,
            nextExpiresAt,
            actor.type(),
            actor.adminUserId(),
            actor.agentId(),
            occurredAt));
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

  private IpBanDetail toDetail(IpBan ban) {
    List<IpBanEventView> events =
        ipBanEventRepository.findByIpBanIdOrderByIdAsc(ban.getId()).stream()
            .map(this::toEventView)
            .toList();
    return new IpBanDetail(
        ban.getId(),
        ban.getNormalizedIp(),
        ban.getStatus(),
        ban.getReason(),
        ban.getEffectiveFrom(),
        ban.getExpiresAt(),
        ban.getStateChangedAt(),
        events);
  }

  private IpBanSummary toSummary(IpBan ban) {
    return new IpBanSummary(
        ban.getId(),
        ban.getNormalizedIp(),
        ban.getStatus(),
        ban.getReason(),
        ban.getEffectiveFrom(),
        ban.getExpiresAt(),
        ban.getStateChangedAt());
  }

  private IpBanEventView toEventView(IpBanEvent event) {
    return new IpBanEventView(
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
}
