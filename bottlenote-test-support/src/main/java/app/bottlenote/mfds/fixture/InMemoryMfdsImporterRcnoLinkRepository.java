package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsImporterRcnoLink;
import app.bottlenote.mfds.domain.MfdsImporterRcnoLinkRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** RCNO별 수입사 연결 근거 도메인 포트의 인메모리 구현체. unit 테스트에서 사용한다. */
public class InMemoryMfdsImporterRcnoLinkRepository implements MfdsImporterRcnoLinkRepository {

  private final Map<String, MfdsImporterRcnoLink> database = new ConcurrentHashMap<>();

  @Override
  public MfdsImporterRcnoLink save(MfdsImporterRcnoLink link) {
    Objects.requireNonNull(link, "link는 null일 수 없습니다.");
    Objects.requireNonNull(link.getRcno(), "rcno는 null일 수 없습니다.");
    database.put(link.getRcno(), link);
    return link;
  }

  @Override
  public Optional<MfdsImporterRcnoLink> findByRcno(String rcno) {
    return Optional.ofNullable(database.get(rcno));
  }

  @Override
  public List<MfdsImporterRcnoLink> findAllByImporterId(Long importerId) {
    return database.values().stream()
        .filter(link -> Objects.equals(link.getImporterId(), importerId))
        .sorted(Comparator.comparing(MfdsImporterRcnoLink::getRcno))
        .toList();
  }

  @Override
  public long countByImporterId(Long importerId) {
    return database.values().stream()
        .filter(link -> Objects.equals(link.getImporterId(), importerId))
        .count();
  }
}
