package app.bottlenote.alcohols.fixture;

import java.math.BigDecimal;
import app.bottlenote.alcohols.constant.AttributeMatch;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeConditionRequest;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholAttributeRepository implements AlcoholAttributeRepository {
  private final Map<Long, AlcoholAttribute> rows = new LinkedHashMap<>();
  private final InMemoryAlcoholAttributeMappingRepository mappings;
  private final AlcoholQueryRepository alcohols;
  private long sequence;

  public InMemoryAlcoholAttributeRepository(
      InMemoryAlcoholAttributeMappingRepository mappings, AlcoholQueryRepository alcohols) {
    this.mappings = mappings;
    this.alcohols = alcohols;
  }

  @Override
  public AlcoholAttribute save(AlcoholAttribute a) {
    if (rows.values().stream()
        .anyMatch(
            other ->
                !Objects.equals(other.getId(), a.getId())
                    && other.getAttributeKey().equals(a.getAttributeKey())))
      throw new DataIntegrityViolationException("uk_alcohol_attributes_key");
    if (existsByDisplayNameAndIdNot(a.getDisplayName(), a.getId()))
      throw new DataIntegrityViolationException("uk_alcohol_attributes_display_name");
    if (a.getId() == null) ReflectionTestUtils.setField(a, "id", ++sequence);
    rows.put(a.getId(), a);
    return a;
  }

  @Override
  public Optional<AlcoholAttribute> findById(Long id) {
    return Optional.ofNullable(rows.get(id));
  }

  @Override
  public Optional<AlcoholAttribute> findByAttributeKey(String key) {
    return rows.values().stream().filter(a -> a.getAttributeKey().equals(key)).findFirst();
  }

  @Override
  public List<AlcoholAttribute> findAllByIdIn(Collection<Long> ids) {
    return rows.values().stream().filter(a -> ids.contains(a.getId())).toList();
  }

  @Override
  public List<AlcoholAttribute> lockByKeys(Collection<String> keys) {
    return rows.values().stream().filter(a -> keys.contains(a.getAttributeKey())).toList();
  }

  @Override
  public List<AlcoholAttribute> lockByIds(Collection<Long> ids) {
    return findAllByIdIn(ids);
  }

  @Override
  public boolean existsByDisplayNameAndIdNot(String name, Long id) {
    return rows.values().stream()
        .anyMatch(
            a -> !Objects.equals(a.getId(), id) && folded(a.getDisplayName()).equals(folded(name)));
  }

  private String folded(String text) {
    return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  @Override
  public OffsetPage<AlcoholAttribute> search(AdminAlcoholAttributeSearchRequest request) {
    Set<Long> ids =
        mappings.findAllByAlcoholIdIn(request.alcoholIds()).stream()
            .map(AlcoholAttributeMapping::getAttributeId)
            .collect(java.util.stream.Collectors.toSet());
    var matched =
        rows.values().stream()
            .filter(a -> request.type() == null || a.getAttributeType() == request.type())
            .filter(
                a ->
                    request.keyword() == null
                        || folded(a.getDisplayName()).contains(folded(request.keyword()))
                        || folded(a.getAttributeKey()).contains(folded(request.keyword())))
            .filter(a -> request.alcoholIds().isEmpty() || ids.contains(a.getId()))
            .toList();
    return new OffsetPage<>(
        matched.stream()
            .skip((long) request.page() * request.size())
            .limit(request.size())
            .toList(),
        matched.size());
  }

  @Override
  public OffsetPage<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request) {
    var matched =
        alcohols.findAll().stream()
            .filter(a -> !a.isDeleted())
            .filter(
                a ->
                    request.keyword() == null
                        || folded(a.getKorName()).contains(folded(request.keyword()))
                        || folded(a.getEngName()).contains(folded(request.keyword())))
            .filter(
                a ->
                    mappings.findAllByAlcoholIdIn(List.of(a.getId())).stream()
                        .filter(m -> rows.containsKey(m.getAttributeId()))
                        .anyMatch(
                            m -> { var v = rows.get(m.getAttributeId()); return
                                (request.type() == null || v.getAttributeType() == request.type())
                                    && (request.key() == null
                                        || v.getAttributeKey().equals(request.key()))
                                    && (request.value() == null
                                        || (request.operator() == AttributeOperator.GTE
                                            ? new BigDecimal(m.getAttributeValue()).compareTo(new BigDecimal(request.value())) >= 0
                                            : m.getAttributeValue().equals(request.value()))); }))
            .filter(
                a ->
                    request.missingKey() == null
                        || mappings.findAllByAlcoholIdIn(List.of(a.getId())).stream()
                            .map(m -> rows.get(m.getAttributeId()))
                            .filter(Objects::nonNull)
                            .noneMatch(v -> v.getAttributeKey().equals(request.missingKey())))
            .sorted(Comparator.comparing(Alcohol::getKorName).thenComparing(Alcohol::getId))
            .map(Alcohol::getId)
            .toList();
    return new OffsetPage<>(
        matched.stream()
            .skip((long) request.page() * request.size())
            .limit(request.size())
            .toList(),
        matched.size());
  }

  @Override
  public OffsetPage<Long> findAlcoholIds(AdminAlcoholCompoundSearchRequest request) {
    var matched = alcohols.findAll().stream().filter(a -> !a.isDeleted())
        .filter(a -> request.keyword() == null || folded(a.getKorName()).contains(folded(request.keyword()))
            || (a.getEngName() != null && folded(a.getEngName()).contains(folded(request.keyword()))))
        .filter(a -> {
          var values = mappings.findAllByAlcoholIdIn(List.of(a.getId())).stream()
              .filter(m -> rows.containsKey(m.getAttributeId())).toList();
          if ((request.linkedOnly() || request.type() != null) && values.stream()
              .noneMatch(m -> request.type() == null || rows.get(m.getAttributeId()).getAttributeType() == request.type())) return false;
          return request.conditions().isEmpty() || (request.match() == AttributeMatch.AND
              ? request.conditions().stream().allMatch(c -> conditionMatches(values, c))
              : request.conditions().stream().anyMatch(c -> conditionMatches(values, c)));
        }).sorted(Comparator.comparing(Alcohol::getKorName).thenComparing(Alcohol::getId)).map(Alcohol::getId).toList();
    return new OffsetPage<>(matched.stream().skip((long) request.page() * request.size()).limit(request.size()).toList(), matched.size());
  }

  private boolean conditionMatches(List<AlcoholAttributeMapping> values, AdminAlcoholAttributeConditionRequest condition) {
    var matching = values.stream().filter(m -> rows.get(m.getAttributeId()).getAttributeKey().equals(condition.key())).toList();
    if (condition.operator() == AttributeSearchOperator.NOT_EXISTS) return matching.isEmpty();
    if (condition.operator() == AttributeSearchOperator.EXISTS) return !matching.isEmpty();
    return matching.stream().anyMatch(m -> switch (condition.operator()) {
      case EQ -> folded(m.getAttributeValue()).equals(folded(condition.value()));
      case NE -> !folded(m.getAttributeValue()).equals(folded(condition.value()));
      case CONTAINS -> folded(m.getAttributeValue()).contains(folded(condition.value()));
      case GT -> new BigDecimal(m.getAttributeValue()).compareTo(new BigDecimal(condition.value())) > 0;
      case GTE -> new BigDecimal(m.getAttributeValue()).compareTo(new BigDecimal(condition.value())) >= 0;
      case LT -> new BigDecimal(m.getAttributeValue()).compareTo(new BigDecimal(condition.value())) < 0;
      case LTE -> new BigDecimal(m.getAttributeValue()).compareTo(new BigDecimal(condition.value())) <= 0;
      default -> false;
    });
  }

  @Override
  public List<AlcoholAttributeKeyCountItem> findKeyStats() {
    return rows.values().stream()
        .sorted(Comparator.comparing(AlcoholAttribute::getAttributeKey))
        .map(
            a ->
                new AlcoholAttributeKeyCountItem(
                    a.getAttributeKey(),
                    a.getAttributeType(),
                    a.getDatatype(),
                    1L,
                    mappings.findAll().stream()
                        .filter(m -> m.getAttributeId().equals(a.getId()))
                        .map(AlcoholAttributeMapping::getAlcoholId)
                        .distinct()
                        .count()))
        .toList();
  }

  @Override
  public List<AlcoholAttributeKeywordCountItem> findKeywordStats() {
    Map<String, Set<Long>> grouped = new TreeMap<>();
    for (var m : mappings.findAll()) {
      var a = rows.get(m.getAttributeId());
      if (a != null && a.getAttributeType() == AttributeType.KEYWORD)
        grouped.computeIfAbsent(m.getAttributeValue(), k -> new HashSet<>()).add(m.getAlcoholId());
    }
    return grouped.entrySet().stream()
        .map(e -> new AlcoholAttributeKeywordCountItem(e.getKey(), (long) e.getValue().size()))
        .sorted(
            Comparator.comparing(AlcoholAttributeKeywordCountItem::alcoholCount)
                .reversed()
                .thenComparing(AlcoholAttributeKeywordCountItem::value))
        .toList();
  }

  @Override
  public void delete(AlcoholAttribute a) {
    deleteAllByIdIn(List.of(a.getId()));
  }

  @Override
  public void deleteAllByIdIn(Collection<Long> ids) {
    ids.forEach(rows::remove);
    mappings.deleteAllByAttributeIdIn(ids);
  }

  public List<AlcoholAttribute> findAll() {
    return List.copyOf(rows.values());
  }
}
