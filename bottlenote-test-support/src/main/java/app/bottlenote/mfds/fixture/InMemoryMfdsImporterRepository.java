package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.domain.MfdsImporterRepository;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/** 수입사 도메인 포트의 인메모리 구현체. unit 테스트에서 사용한다. */
public class InMemoryMfdsImporterRepository implements MfdsImporterRepository {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final Map<Long, MfdsImporter> database = new ConcurrentHashMap<>();

  @Override
  public MfdsImporter save(MfdsImporter importer) {
    Objects.requireNonNull(importer, "importer는 null일 수 없습니다.");
    if (importer.getId() == null) {
      ReflectionTestUtils.setField(importer, "id", idGenerator.getAndIncrement());
    }
    database.put(importer.getId(), importer);
    return importer;
  }

  @Override
  public Optional<MfdsImporter> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<MfdsImporter> findByOfficialBusinessCode(String officialBusinessCode) {
    return database.values().stream()
        .filter(importer -> Objects.equals(importer.getOfficialBusinessCode(), officialBusinessCode))
        .findFirst();
  }

  @Override
  public List<MfdsImporter> searchByCriteria(MfdsImporterSearchCriteria criteria) {
    return database.values().stream()
        .filter(importer -> matches(importer, criteria))
        .filter(importer -> !criteria.hasCursor() || importer.getId() < criteria.cursor())
        .sorted(Comparator.comparing(MfdsImporter::getId).reversed())
        .limit(criteria.fetchLimit())
        .toList();
  }

  @Override
  public long countByCriteria(MfdsImporterSearchCriteria criteria) {
    return database.values().stream().filter(importer -> matches(importer, criteria)).count();
  }

  private boolean matches(MfdsImporter importer, MfdsImporterSearchCriteria criteria) {
    if (criteria.adminStatus() != null && importer.getAdminStatus() != criteria.adminStatus()) {
      return false;
    }
    if (criteria.keyword() != null) {
      String keyword = criteria.keyword().toLowerCase(Locale.ROOT);
      return containsIgnoreCase(importer.getBusinessName(), keyword)
          || containsIgnoreCase(importer.getLicenseNo(), keyword)
          || containsIgnoreCase(importer.getOfficialBusinessCode(), keyword);
    }
    return true;
  }

  private boolean containsIgnoreCase(String value, String lowerKeyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
  }
}
