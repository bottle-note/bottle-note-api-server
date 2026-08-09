package app.bottlenote.accesscontrol.service;

import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 180일이 지난 종료 밴과 보안 signal을 FK 순서로 정리한다. */
@Service
@RequiredArgsConstructor
public class IpBanRetentionService {

  static final int BATCH_SIZE = 200;
  private static final int RETENTION_DAYS = 180;

  private final IpBanRepository ipBanRepository;
  private final IpBanEventRepository ipBanEventRepository;
  private final IpSecuritySignalRepository ipSecuritySignalRepository;
  private final Clock clock;

  @Transactional
  public void purgeExpiredData() {
    LocalDateTime cutoff = now().minusDays(RETENTION_DAYS);
    deleteExpiredSignals(cutoff);
    deleteTerminatedBans(cutoff);
  }

  private void deleteExpiredSignals(LocalDateTime cutoff) {
    while (true) {
      List<Long> ids =
          ipSecuritySignalRepository.findByCreateAtBeforeOrderByIdAsc(cutoff, BATCH_SIZE).stream()
              .map(IpSecuritySignal::getId)
              .toList();
      if (ids.isEmpty()) {
        return;
      }
      ipSecuritySignalRepository.deleteByIds(ids);
    }
  }

  private void deleteTerminatedBans(LocalDateTime cutoff) {
    while (true) {
      List<Long> ids =
          ipBanRepository.findTerminatedBefore(cutoff, BATCH_SIZE).stream()
              .map(IpBan::getId)
              .toList();
      if (ids.isEmpty()) {
        return;
      }
      ipSecuritySignalRepository.deleteByIpBanIdIn(ids);
      ipBanEventRepository.deleteByIpBanIdIn(ids);
      ipBanRepository.deleteByIds(ids);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
  }
}
