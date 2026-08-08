package app.bottlenote.accesscontrol.fixture;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBan;
import app.bottlenote.accesscontrol.domain.IpBanRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryIpBanRepository implements IpBanRepository {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final Map<Long, IpBan> database = new ConcurrentHashMap<>();

  @Override
  public IpBan save(IpBan ipBan) {
    Objects.requireNonNull(ipBan, "ipBan은 null일 수 없습니다.");
    if (ipBan.getId() == null) {
      ReflectionTestUtils.setField(ipBan, "id", idGenerator.getAndIncrement());
    }
    database.put(ipBan.getId(), ipBan);
    return ipBan;
  }

  @Override
  public Optional<IpBan> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<IpBan> findByNormalizedIp(String normalizedIp) {
    return database.values().stream()
        .filter(ban -> ban.getNormalizedIp().equals(normalizedIp))
        .findFirst();
  }

  @Override
  public Optional<IpBan> findByNormalizedIpForUpdate(String normalizedIp) {
    return findByNormalizedIp(normalizedIp);
  }

  @Override
  public List<IpBan> findByStatusOrderByStateChangedAtDesc(IpBanStatus status, int limit) {
    return database.values().stream()
        .filter(ban -> ban.getStatus() == status)
        .sorted(latestFirst())
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public List<IpBan> findAllOrderByStateChangedAtDesc(int limit) {
    return database.values().stream()
        .sorted(latestFirst())
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public List<IpBan> findActiveAfter(LocalDateTime expiresAt, Long id, int limit) {
    return database.values().stream()
        .filter(IpBan::isActive)
        .filter(
            ban ->
                expiresAt == null
                    || ban.getExpiresAt().isAfter(expiresAt)
                    || (ban.getExpiresAt().isEqual(expiresAt) && ban.getId() > id))
        .sorted(Comparator.comparing(IpBan::getExpiresAt).thenComparing(IpBan::getId))
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public List<IpBan> findInactiveAfter(LocalDateTime stateChangedAt, Long id, int limit) {
    return database.values().stream()
        .filter(ban -> !ban.isActive())
        .filter(
            ban ->
                stateChangedAt == null
                    || ban.getStateChangedAt().isAfter(stateChangedAt)
                    || (ban.getStateChangedAt().isEqual(stateChangedAt) && ban.getId() > id))
        .sorted(Comparator.comparing(IpBan::getStateChangedAt).thenComparing(IpBan::getId))
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public List<IpBan> findTerminatedBefore(LocalDateTime cutoff, int limit) {
    return database.values().stream()
        .filter(ban -> !ban.isActive() && ban.getStateChangedAt().isBefore(cutoff))
        .sorted(Comparator.comparing(IpBan::getStateChangedAt).thenComparing(IpBan::getId))
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public int deleteByIds(List<Long> ids) {
    int count = 0;
    for (Long id : ids) {
      if (database.remove(id) != null) {
        count++;
      }
    }
    return count;
  }

  public List<IpBan> findAll() {
    return new ArrayList<>(database.values());
  }

  public void clear() {
    database.clear();
    idGenerator.set(1L);
  }

  private static Comparator<IpBan> latestFirst() {
    return Comparator.comparing(IpBan::getStateChangedAt)
        .thenComparing(IpBan::getId)
        .reversed();
  }
}
