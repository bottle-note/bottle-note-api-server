package app.bottlenote.accesscontrol.fixture;

import app.bottlenote.accesscontrol.domain.IpBanEvent;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryIpBanEventRepository implements IpBanEventRepository {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final List<IpBanEvent> database = new ArrayList<>();

  @Override
  public IpBanEvent save(IpBanEvent event) {
    Objects.requireNonNull(event, "event는 null일 수 없습니다.");
    if (event.getId() != null) {
      throw new IllegalArgumentException("이미 저장된 이벤트는 다시 저장할 수 없습니다.");
    }
    ReflectionTestUtils.setField(event, "id", idGenerator.getAndIncrement());
    database.add(event);
    return event;
  }

  @Override
  public List<IpBanEvent> findByIpBanIdOrderByIdAsc(Long ipBanId) {
    return database.stream()
        .filter(event -> event.getIpBanId().equals(ipBanId))
        .sorted(Comparator.comparing(IpBanEvent::getId))
        .toList();
  }

  @Override
  public long findLatestIdByIpBanId(Long ipBanId) {
    return database.stream()
        .filter(event -> event.getIpBanId().equals(ipBanId))
        .mapToLong(IpBanEvent::getId)
        .max()
        .orElse(0L);
  }

  @Override
  public int deleteByIpBanIdIn(List<Long> ipBanIds) {
    int size = database.size();
    database.removeIf(event -> ipBanIds.contains(event.getIpBanId()));
    return size - database.size();
  }

  public List<IpBanEvent> findAll() {
    return List.copyOf(database);
  }

  public void clear() {
    database.clear();
    idGenerator.set(1L);
  }
}
