package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
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
    reconcileActiveBans();
    removeStaleRedisBans();
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

  private void removeStaleRedisBans() {
    for (AccessControlStore.BanInfo redisBan : accessControlStore.listBans(BATCH_SIZE)) {
      try {
        IpBanDetailResponse detail = ipBanFacade.findByIp(redisBan.ip()).orElse(null);
        if (detail == null) {
          accessControlStore.removeUnversionedBan(redisBan.ip());
          continue;
        }
        if (detail.status() != IpBanStatus.ACTIVE || !detail.expiresAt().isAfter(now())) {
          accessControlStore.projectUnban(redisBan.ip(), detail.events().getLast().id());
        }
      } catch (RuntimeException exception) {
        log.warn("IP ban stale Redis cleanup failed ip={}", redisBan.ip(), exception);
      }
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
  }
}
