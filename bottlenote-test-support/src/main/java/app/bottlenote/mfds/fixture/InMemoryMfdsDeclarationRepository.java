package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsDeclarationRepository;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import app.bottlenote.mfds.dto.dsl.MfdsPublicAlcoholSearchCriteria;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
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

  /** 인메모리 구현에는 행 잠금 개념이 없으므로 일반 조회와 동일하게 동작한다. */
  @Override
  public Optional<MfdsDeclaration> findByIdForUpdate(Long id) {
    return findById(id);
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

  @Override
  public boolean existsByImporterId(Long importerId) {
    return database.values().stream()
        .anyMatch(declaration -> Objects.equals(declaration.getImporterId(), importerId));
  }

  @Override
  public List<MfdsDeclaration> searchPublicAlcohols(MfdsPublicAlcoholSearchCriteria criteria) {
    return database.values().stream()
        .filter(declaration -> matchesPublic(declaration, criteria))
        .sorted(
            Comparator.comparing(
                    MfdsDeclaration::getProcessedDate,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MfdsDeclaration::getId, Comparator.reverseOrder()))
        .limit(criteria.fetchLimit())
        .toList();
  }

  @Override
  public List<MfdsPublicCountryItem> findExportCountries() {
    return database.values().stream()
        .filter(declaration -> declaration.getExportCountryAlpha2() != null)
        .collect(
            java.util.stream.Collectors.toMap(
                MfdsDeclaration::getExportCountryAlpha2,
                declaration ->
                    new MfdsPublicCountryItem(
                        declaration.getExportCountryAlpha2(),
                        declaration.getExportCountryNameKo(),
                        declaration.getExportCountryNameEn()),
                (left, right) -> left,
                java.util.LinkedHashMap::new))
        .values()
        .stream()
        .sorted(
            Comparator.comparing(
                item -> item.nameKo() == null ? "" : item.nameKo(), String.CASE_INSENSITIVE_ORDER))
        .toList();
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

  private boolean matchesPublic(
      MfdsDeclaration declaration, MfdsPublicAlcoholSearchCriteria criteria) {
    if (criteria.alcoholNameKo() != null
        && !Objects.equals(declaration.getAlcoholNameKo(), criteria.alcoholNameKo())) {
      return false;
    }
    if (criteria.alcoholId() != null
        && !Objects.equals(declaration.getSelectedAlcoholId(), criteria.alcoholId())) {
      return false;
    }
    if (criteria.importerId() != null
        && !Objects.equals(declaration.getImporterId(), criteria.importerId())) {
      return false;
    }
    if (criteria.exportCountry() != null
        && !Objects.equals(declaration.getExportCountryAlpha2(), criteria.exportCountry())) {
      return false;
    }
    if (criteria.alcoholCategoryKo() != null
        && !Objects.equals(declaration.getAlcoholCategoryKo(), criteria.alcoholCategoryKo())) {
      return false;
    }
    if (criteria.processedDateFrom() != null
        && (declaration.getProcessedDate() == null
            || declaration.getProcessedDate().isBefore(criteria.processedDateFrom()))) {
      return false;
    }
    if (criteria.processedDateTo() != null
        && (declaration.getProcessedDate() == null
            || declaration.getProcessedDate().isAfter(criteria.processedDateTo()))) {
      return false;
    }
    if (!matchesPublicTokens(declaration, criteria.searchTokens())) {
      return false;
    }
    return isAfterPublicCursor(
        declaration, criteria.cursorProcessedDate(), criteria.cursorId());
  }

  private boolean matchesPublicTokens(MfdsDeclaration declaration, List<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return true;
    }
    return tokens.stream()
        .allMatch(
            token ->
                containsIgnoreCase(declaration.getRcno(), token)
                    || containsIgnoreCase(declaration.getBaseProductNameKo(), token)
                    || containsIgnoreCase(declaration.getBaseProductNameEn(), token)
                    || containsIgnoreCase(declaration.getSkuDisplayNameKo(), token)
                    || containsIgnoreCase(declaration.getSkuDisplayNameEn(), token)
                    || containsIgnoreCase(declaration.getAlcoholNameKo(), token)
                    || containsIgnoreCase(declaration.getAlcoholNameEn(), token)
                    || containsIgnoreCase(declaration.getAlcoholCategoryKo(), token)
                    || containsIgnoreCase(declaration.getAlcoholCategoryEn(), token)
                    || containsIgnoreCase(declaration.getManufacturerName(), token)
                    || containsIgnoreCase(declaration.getImporterBaseName(), token));
  }

  private boolean isAfterPublicCursor(
      MfdsDeclaration declaration, java.time.LocalDate cursorDate, Long cursorId) {
    if (cursorId == null) {
      return true;
    }
    java.time.LocalDate processedDate = declaration.getProcessedDate();
    if (cursorDate == null) {
      return processedDate == null && declaration.getId() < cursorId;
    }
    if (processedDate == null) {
      return true;
    }
    int compared = processedDate.compareTo(cursorDate);
    if (compared < 0) {
      return true;
    }
    return compared == 0 && declaration.getId() < cursorId;
  }
}
