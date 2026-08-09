package app.bottlenote.accesscontrol.fixture;

import app.bottlenote.accesscontrol.domain.IpSecuritySignal;
import app.bottlenote.accesscontrol.domain.IpSecuritySignalRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryIpSecuritySignalRepository implements IpSecuritySignalRepository {
  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final Map<Long, IpSecuritySignal> database = new ConcurrentHashMap<>();

  @Override
  public IpSecuritySignal save(IpSecuritySignal signal) {
    Objects.requireNonNull(signal, "signal은 null일 수 없습니다.");
    if (signal.getId() == null) {
      ReflectionTestUtils.setField(signal, "id", idGenerator.getAndIncrement());
    }
    database.put(signal.getId(), signal);
    return signal;
  }

  @Override
  public Optional<IpSecuritySignal> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<IpSecuritySignal> findByIdForUpdate(Long id) {
    return findById(id);
  }

  @Override
  public List<IpSecuritySignal> findByNormalizedIpOrderByIdDesc(String normalizedIp, int limit) {
    return database.values().stream()
        .filter(signal -> signal.getNormalizedIp().equals(normalizedIp))
        .sorted(Comparator.comparing(IpSecuritySignal::getId).reversed())
        .limit(Math.max(limit, 1))
        .toList();
  }

  @Override
  public List<IpSecuritySignal> findByCreateAtBeforeOrderByIdAsc(LocalDateTime cutoff, int limit) {
    return database.values().stream()
        .filter(signal -> signal.getCreateAt() != null && signal.getCreateAt().isBefore(cutoff))
        .sorted(Comparator.comparing(IpSecuritySignal::getId))
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

  @Override
  public int deleteByIpBanIdIn(List<Long> ipBanIds) {
    int size = database.size();
    database.values().removeIf(signal -> ipBanIds.contains(signal.getIpBanId()));
    return size - database.size();
  }

  public List<IpSecuritySignal> findAll() {
    return database.values().stream().sorted(Comparator.comparing(IpSecuritySignal::getId)).toList();
  }
}
