package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** MySQL 상태를 bounded batch로 Redis enforcement에 수렴시킨다. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "bottlenote.access-control",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class IpBanReconciliationService {

  static final int BATCH_SIZE = 200;
  private static final String EXPIRE_REASON = "차단 기간 만료";

  private final IpBanRepository ipBanRepository;
  private final IpBanEventRepository ipBanEventRepository;
  private final IpBanFacade ipBanFacade;
  private final AccessControlStore accessControlStore;
  private final Clock clock;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void reconcile() {
    reconcile(null);
  }

  /**
   * 활성 차단은 매 실행마다 복구하고, 종료 차단은 전달받은 keyset 이후 최대 한 batch만 처리한다.
   *
   * @return 다음 실행에 사용할 cursor. 종료에 도달하면 {@code null}이다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public InactiveCursor reconcile(InactiveCursor inactiveCursor) {
    reconcileActiveBans();
    InactiveReconciliationResult inactiveResult = reconcileInactiveBans(inactiveCursor);
    removeLegacyRedisBans(BATCH_SIZE - inactiveResult.processedCount());
    return inactiveResult.nextCursor();
  }

  private void reconcileActiveBans() {
    LocalDateTime cursorExpiresAt = null;
    Long cursorId = null;
    LocalDateTime now = now();
    while (true) {
      List<IpBan> batch = ipBanRepository.findActiveAfter(cursorExpiresAt, cursorId, BATCH_SIZE);
      if (batch.isEmpty()) {
        return;
      }
      for (IpBan ban : batch) {
        reconcile(ban, now);
      }
      IpBan last = batch.getLast();
      cursorExpiresAt = last.getExpiresAt();
      cursorId = last.getId();
      if (batch.size() < BATCH_SIZE) {
        return;
      }
    }
  }

  private void reconcile(IpBan ban, LocalDateTime now) {
    if (!ban.getExpiresAt().isAfter(now)) {
      ipBanFacade.expire(ban.getNormalizedIp(), EXPIRE_REASON);
      return;
    }
    try {
      long eventId = ipBanEventRepository.findLatestIdByIpBanId(ban.getId());
      accessControlStore.projectBan(
          ban.getNormalizedIp(),
          Duration.between(now, ban.getExpiresAt()),
          ban.getReason(),
          eventId);
    } catch (RuntimeException exception) {
      log.warn("IP ban reconciliation projection failed ip={}", ban.getNormalizedIp(), exception);
    }
  }

  private InactiveReconciliationResult reconcileInactiveBans(InactiveCursor cursor) {
    LocalDateTime stateChangedAt = cursor == null ? null : cursor.stateChangedAt();
    Long id = cursor == null ? null : cursor.id();
    List<IpBan> batch = ipBanRepository.findInactiveAfter(stateChangedAt, id, BATCH_SIZE);
    if (batch.isEmpty()) {
      return new InactiveReconciliationResult(null, 0);
    }
    for (IpBan ban : batch) {
      try {
        accessControlStore.projectUnban(
            ban.getNormalizedIp(), ipBanEventRepository.findLatestIdByIpBanId(ban.getId()));
      } catch (RuntimeException exception) {
        log.warn("IP ban inactive reconciliation failed ip={}", ban.getNormalizedIp(), exception);
      }
    }
    if (batch.size() < BATCH_SIZE) {
      return new InactiveReconciliationResult(null, batch.size());
    }
    IpBan last = batch.getLast();
    return new InactiveReconciliationResult(
        new InactiveCursor(last.getStateChangedAt(), last.getId()), batch.size());
  }

  private void removeLegacyRedisBans(int max) {
    if (max <= 0) {
      return;
    }
    for (AccessControlStore.BanInfo redisBan : accessControlStore.listBans(max)) {
      try {
        IpBanDetailResponse detail = ipBanFacade.findByIp(redisBan.ip()).orElse(null);
        if (detail == null) {
          accessControlStore.removeUnversionedBan(redisBan.ip());
        }
      } catch (RuntimeException exception) {
        log.warn("IP ban stale Redis cleanup failed ip={}", redisBan.ip(), exception);
      }
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
  }

  /** JDBC Quartz JobDataMap에 문자열로 보관할 수 있는 종료 차단 keyset 위치. */
  public record InactiveCursor(LocalDateTime stateChangedAt, long id) {}

  private record InactiveReconciliationResult(InactiveCursor nextCursor, int processedCount) {}
}
