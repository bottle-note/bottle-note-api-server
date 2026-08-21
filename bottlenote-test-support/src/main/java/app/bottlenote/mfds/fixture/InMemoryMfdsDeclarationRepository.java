package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/** 신고 정제 데이터 도메인 포트의 인메모리 구현체. unit 테스트에서 사용한다. */
public class InMemoryMfdsDeclarationRepository implements MfdsDeclarationRepository {

  private final AtomicLong idGenerator = new AtomicLong(1L);
  private final Map<Long, MfdsDeclaration> database = new ConcurrentHashMap<>();

  @Override
  public MfdsDeclaration save(MfdsDeclaration declaration) {
    Objects.requireNonNull(declaration, "declaration은 null일 수 없습니다.");
    if (declaration.getId() == null) {
      ReflectionTestUtils.setField(declaration, "id", idGenerator.getAndIncrement());
    }
    database.put(declaration.getId(), declaration);
    return declaration;
  }

  @Override
  public Optional<MfdsDeclaration> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<MfdsDeclaration> findByRcno(String rcno) {
    return database.values().stream()
        .filter(declaration -> Objects.equals(declaration.getRcno(), rcno))
        .findFirst();
  }

  @Override
  public List<MfdsDeclaration> searchByCriteria(MfdsDeclarationSearchCriteria criteria) {
    return database.values().stream()
        .filter(declaration -> matches(declaration, criteria))
        .filter(declaration -> !criteria.hasCursor() || declaration.getId() < criteria.cursor())
        .sorted(Comparator.comparing(MfdsDeclaration::getId).reversed())
        .limit(criteria.fetchLimit())
        .toList();
  }

  @Override
  public long countByCriteria(MfdsDeclarationSearchCriteria criteria) {
    return database.values().stream().filter(declaration -> matches(declaration, criteria)).count();
  }

  private boolean matches(MfdsDeclaration declaration, MfdsDeclarationSearchCriteria criteria) {
    if (criteria.normalizationStatus() != null
        && declaration.getNormalizationStatus() != criteria.normalizationStatus()) {
      return false;
    }
    if (criteria.alcoholMatched() != null
        && criteria.alcoholMatched() != (declaration.getSelectedAlcoholId() != null)) {
      return false;
    }
    if (criteria.alcoholMatchDecision() != null
        && !Objects.equals(declaration.getAlcoholMatchDecision(), criteria.alcoholMatchDecision())) {
      return false;
    }
    if (criteria.importerId() != null
        && !Objects.equals(declaration.getImporterId(), criteria.importerId())) {
      return false;
    }
    if (criteria.keyword() != null) {
      String keyword = criteria.keyword().toLowerCase(Locale.ROOT);
      return containsIgnoreCase(declaration.getNameSearchKeyKo(), keyword)
          || containsIgnoreCase(declaration.getNameSearchKeyEn(), keyword)
          || containsIgnoreCase(declaration.getRcno(), keyword);
    }
    return true;
  }

  private boolean containsIgnoreCase(String value, String lowerKeyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
  }
}
