package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_ATTRIBUTE_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_ATTRIBUTE_UPDATED;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.constant.ValueProvider;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeyItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeywordItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AdminAuditPrincipalItem;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminAlcoholAttributeService implements AdminAlcoholAttributeService {

  private final AlcoholAttributeRepository attributeRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeItem> findAll(AdminAlcoholAttributeSearchRequest request) {
    return attributeRepository.findAllByAlcoholIdIn(request.alcoholIds()).stream()
        .map(this::toItem)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeSummaryItem> summarize(
      AdminAlcoholAttributeSearchRequest request) {
    long selectedCount = request.alcoholIds().stream().distinct().count();
    Map<SummaryGroupKey, List<AlcoholAttribute>> grouped = new LinkedHashMap<>();
    for (AlcoholAttribute row : attributeRepository.findAllByAlcoholIdIn(request.alcoholIds())) {
      String valueKey = row.getDatatype().isNumeric() ? null : row.getAttributeValue();
      SummaryGroupKey groupKey =
          new SummaryGroupKey(
              row.getAttributeKey(), row.getAttributeType(), row.getDatatype(), valueKey);
      grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(row);
    }
    return grouped.entrySet().stream()
        .map(entry -> toSummary(entry.getKey(), entry.getValue(), selectedCount))
        .sorted(
            Comparator.comparing(AdminAlcoholAttributeSummaryItem::key)
                .thenComparing(item -> item.type().name())
                .thenComparing(item -> item.datatype().name())
                .thenComparing(
                    AdminAlcoholAttributeSummaryItem::value,
                    Comparator.nullsFirst(String::compareTo)))
        .toList();
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeCreateResponse create(AdminAlcoholAttributeCreateRequest request) {
    requireAlcohols(request.alcoholIds());
    Set<String> seen = new HashSet<>();
    for (AlcoholAttribute row : attributeRepository.findAllByAlcoholIdIn(request.alcoholIds())) {
      seen.add(token(row.getAlcoholId(), row.getAttributeKey(), row.getAttributeValue()));
    }
    int added = 0;
    int skipped = 0;
    List<Long> createdIds = new ArrayList<>();
    for (Long alcoholId : request.alcoholIds()) {
      AlcoholAttribute candidate =
          AlcoholAttribute.create(
              alcoholId,
              request.key(),
              request.type(),
              request.datatype(),
              request.value(),
              request.description(),
              request.basedAt());
      if (!seen.add(token(alcoholId, candidate.getAttributeKey(), candidate.getAttributeValue()))) {
        skipped++;
        continue;
      }
      createdIds.add(attributeRepository.save(candidate).getId());
      added++;
    }
    return new AdminAlcoholAttributeCreateResponse(added, skipped, List.copyOf(createdIds));
  }

  @Override
  @Transactional
  public AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request) {
    AlcoholAttribute attribute = findAttribute(id);
    attribute.update(
        request.type(),
        request.datatype(),
        request.value(),
        request.description(),
        request.basedAt());
    attributeRepository.save(attribute);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_UPDATED, id);
  }

  @Override
  @Transactional
  public AdminResultResponse delete(Long id) {
    AlcoholAttribute attribute = findAttribute(id);
    attributeRepository.delete(attribute);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_DELETED, id);
  }

  @Override
  @Transactional
  public List<AdminAlcoholAttributeItem> deleteBatch(
      AdminAlcoholAttributeBatchDeleteRequest request) {
    List<AlcoholAttribute> rows = attributeRepository.findAllByIdIn(request.ids());
    List<AdminAlcoholAttributeItem> deleted = rows.stream().map(this::toItem).toList();
    if (!rows.isEmpty()) {
      attributeRepository.deleteAllByIdIn(rows.stream().map(AlcoholAttribute::getId).toList());
    }
    return deleted;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeKeyItem> findKeys() {
    Map<KeyGroup, List<AlcoholAttribute>> grouped = new LinkedHashMap<>();
    for (AttributeType type : AttributeType.values()) {
      for (AlcoholAttribute row : attributeRepository.findAllByAttributeType(type)) {
        KeyGroup group =
            new KeyGroup(row.getAttributeKey(), row.getAttributeType(), row.getDatatype());
        grouped.computeIfAbsent(group, key -> new ArrayList<>()).add(row);
      }
    }
    return grouped.entrySet().stream()
        .map(entry -> toKeyItem(entry.getKey(), entry.getValue()))
        .sorted(
            Comparator.comparing(AdminAlcoholAttributeKeyItem::key)
                .thenComparing(item -> item.type().name())
                .thenComparing(item -> item.datatype().name()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeKeywordItem> findKeywords() {
    Map<String, Set<Long>> alcoholsByValue = new LinkedHashMap<>();
    for (AlcoholAttribute row : attributeRepository.findAllByAttributeType(AttributeType.KEYWORD)) {
      alcoholsByValue
          .computeIfAbsent(row.getAttributeValue(), value -> new HashSet<>())
          .add(row.getAlcoholId());
    }
    return alcoholsByValue.entrySet().stream()
        .map(entry -> new AdminAlcoholAttributeKeywordItem(entry.getKey(), entry.getValue().size()))
        .sorted(
            Comparator.comparingLong(AdminAlcoholAttributeKeywordItem::alcoholCount)
                .reversed()
                .thenComparing(AdminAlcoholAttributeKeywordItem::value))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminAlcoholAttributeEnumsResponse findEnums() {
    return new AdminAlcoholAttributeEnumsResponse(
        labels(AttributeType.values()),
        labels(AttributeDatatype.values()),
        labels(ValueProvider.values()));
  }

  private void requireAlcohols(List<Long> alcoholIds) {
    List<Long> distinct = alcoholIds.stream().distinct().toList();
    Set<Long> found =
        alcoholQueryRepository.findAllByIdIn(distinct).stream()
            .map(Alcohol::getId)
            .collect(Collectors.toSet());
    if (found.size() != distinct.size()) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
  }

  private AlcoholAttribute findAttribute(Long id) {
    return attributeRepository
        .findById(id)
        .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND));
  }

  private AdminAlcoholAttributeSummaryItem toSummary(
      SummaryGroupKey key, List<AlcoholAttribute> rows, long selectedCount) {
    boolean numeric = key.datatype().isNumeric();
    boolean allSame =
        numeric
            ? sameNumber(rows)
            : rows.stream().map(AlcoholAttribute::getAttributeValue).distinct().count() == 1;
    BigDecimal min = null;
    BigDecimal max = null;
    if (numeric) {
      min =
          rows.stream()
              .map(row -> new BigDecimal(row.getAttributeValue()))
              .min(BigDecimal::compareTo)
              .orElseThrow();
      max =
          rows.stream()
              .map(row -> new BigDecimal(row.getAttributeValue()))
              .max(BigDecimal::compareTo)
              .orElseThrow();
    }
    String value = !numeric || allSame ? rows.get(0).getAttributeValue() : null;
    List<Long> alcoholIds =
        rows.stream().map(AlcoholAttribute::getAlcoholId).distinct().sorted().toList();
    return new AdminAlcoholAttributeSummaryItem(
        key.key(),
        value,
        key.type(),
        key.datatype(),
        alcoholIds,
        rows.size(),
        selectedCount,
        min,
        max,
        allSame);
  }

  private boolean sameNumber(List<AlcoholAttribute> rows) {
    BigDecimal first = new BigDecimal(rows.get(0).getAttributeValue());
    return rows.stream()
        .allMatch(row -> first.compareTo(new BigDecimal(row.getAttributeValue())) == 0);
  }

  private AdminAlcoholAttributeKeyItem toKeyItem(KeyGroup key, List<AlcoholAttribute> rows) {
    long alcoholCount = rows.stream().map(AlcoholAttribute::getAlcoholId).distinct().count();
    return new AdminAlcoholAttributeKeyItem(
        key.key(), key.type(), key.datatype(), rows.size(), alcoholCount);
  }

  private AdminAlcoholAttributeItem toItem(AlcoholAttribute attribute) {
    return new AdminAlcoholAttributeItem(
        attribute.getId(),
        attribute.getAlcoholId(),
        attribute.getAttributeKey(),
        attribute.getAttributeType(),
        attribute.getDatatype(),
        attribute.getAttributeValue(),
        attribute.getDescription(),
        attribute.getBasedAt(),
        attribute.getCreateAt(),
        attribute.getLastModifyAt(),
        toPrincipal(attribute.getCreatePrincipal()),
        toPrincipal(attribute.getLastModifyPrincipal()));
  }

  private AdminAuditPrincipalItem toPrincipal(AuditPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return new AdminAuditPrincipalItem(
        principal.getId(), principal.getType(), principal.getEmail());
  }

  private List<AdminCodeLabelItem> labels(AttributeType[] values) {
    return Arrays.stream(values)
        .map(
            value ->
                label(value.name(), value.getKorName(), value.getEngName(), value.getDescription()))
        .toList();
  }

  private List<AdminCodeLabelItem> labels(AttributeDatatype[] values) {
    return Arrays.stream(values)
        .map(
            value ->
                label(value.name(), value.getKorName(), value.getEngName(), value.getDescription()))
        .toList();
  }

  private List<AdminCodeLabelItem> labels(ValueProvider[] values) {
    return Arrays.stream(values)
        .map(
            value ->
                label(value.name(), value.getKorName(), value.getEngName(), value.getDescription()))
        .toList();
  }

  private AdminCodeLabelItem label(
      String code, String korName, String engName, String description) {
    return new AdminCodeLabelItem(code, korName, engName, description);
  }

  private static String token(Long alcoholId, String key, String value) {
    return alcoholId + "\u0000" + key + "\u0000" + value;
  }

  private record SummaryGroupKey(
      String key, AttributeType type, AttributeDatatype datatype, String value) {}

  private record KeyGroup(String key, AttributeType type, AttributeDatatype datatype) {}
}
