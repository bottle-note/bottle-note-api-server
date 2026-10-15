package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_DUPLICATE;
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
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminAuditPrincipalItem;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
  @Transactional(readOnly = true)
  public Page<AdminAttributedAlcoholItem> searchAlcohols(
      AdminAttributedAlcoholSearchRequest request) {
    Page<Long> page =
        attributeRepository.findAlcoholIds(request, PageRequest.of(request.page(), request.size()));
    List<Long> alcoholIds = page.getContent();
    if (alcoholIds.isEmpty()) {
      return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
    }
    Map<Long, Alcohol> alcohols =
        alcoholQueryRepository.findAllByIdIn(alcoholIds).stream()
            .collect(Collectors.toMap(Alcohol::getId, Function.identity()));
    Map<Long, List<AdminAlcoholAttributeItem>> rowsByAlcohol =
        attributeRepository.findAllByAlcoholIdIn(alcoholIds).stream()
            .collect(
                Collectors.groupingBy(
                    AlcoholAttribute::getAlcoholId,
                    Collectors.mapping(this::toItem, Collectors.toList())));
    List<AdminAttributedAlcoholItem> content =
        alcoholIds.stream()
            .map(
                alcoholId -> {
                  Alcohol alcohol = alcohols.get(alcoholId);
                  return new AdminAttributedAlcoholItem(
                      alcoholId,
                      alcohol == null ? null : alcohol.getKorName(),
                      alcohol == null ? null : alcohol.getEngName(),
                      alcohol == null ? null : alcohol.getKorCategory(),
                      alcohol == null ? null : alcohol.getImageUrl(),
                      rowsByAlcohol.getOrDefault(alcoholId, List.of()));
                })
            .toList();
    return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeCreateResponse create(AdminAlcoholAttributeCreateRequest request) {
    requireAlcohols(request.alcoholIds());
    // 한 요청 안에서 key·value는 고정이라 요청 내 중복은 위스키 ID만 보면 된다
    Set<Long> seenAlcoholIds = new HashSet<>();
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
      if (!seenAlcoholIds.add(alcoholId)
          || attributeRepository.existsByAlcoholIdAndAttributeKeyAndAttributeValue(
              alcoholId, candidate.getAttributeKey(), candidate.getAttributeValue())) {
        skipped++;
        continue;
      }
      saveUnique(candidate);
      createdIds.add(candidate.getId());
      added++;
    }
    return new AdminAlcoholAttributeCreateResponse(added, skipped, List.copyOf(createdIds));
  }

  @Override
  @Transactional
  public AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request) {
    AlcoholAttribute attribute = requireAttribute(id);
    AlcoholAttribute normalized =
        AlcoholAttribute.create(
            attribute.getAlcoholId(),
            attribute.getAttributeKey(),
            request.type(),
            request.datatype(),
            request.value(),
            request.description(),
            request.basedAt());
    if (attributeRepository.existsByAlcoholIdAndAttributeKeyAndAttributeValueAndIdNot(
        attribute.getAlcoholId(),
        normalized.getAttributeKey(),
        normalized.getAttributeValue(),
        attribute.getId())) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_DUPLICATE);
    }
    attribute.update(
        request.type(),
        request.datatype(),
        request.value(),
        request.description(),
        request.basedAt());
    saveUnique(attribute);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_UPDATED, id);
  }

  @Override
  @Transactional
  public AdminResultResponse delete(Long id) {
    AlcoholAttribute attribute = requireAttribute(id);
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
  public List<AlcoholAttributeKeyCountItem> findKeys() {
    return attributeRepository.findKeyStats();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlcoholAttributeKeywordCountItem> findKeywords() {
    return attributeRepository.findKeywordStats();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminAlcoholAttributeEnumsResponse findEnums() {
    return new AdminAlcoholAttributeEnumsResponse(
        AdminCodeLabelItem.listOf(AttributeType.values()),
        AdminCodeLabelItem.listOf(AttributeDatatype.values()),
        AdminCodeLabelItem.listOf(ValueProvider.values()));
  }

  private void requireAlcohols(List<Long> alcoholIds) {
    List<Long> distinct = alcoholIds.stream().distinct().toList();
    Set<Long> found =
        alcoholQueryRepository.findAllByIdIn(distinct).stream()
            .filter(alcohol -> !alcohol.isDeleted())
            .map(Alcohol::getId)
            .collect(Collectors.toSet());
    if (found.size() != distinct.size()) {
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    }
  }

  private AlcoholAttribute requireAttribute(Long id) {
    return attributeRepository
        .findById(id)
        .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND));
  }

  private void saveUnique(AlcoholAttribute attribute) {
    try {
      attributeRepository.save(attribute);
    } catch (DataIntegrityViolationException ex) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_DUPLICATE);
    }
  }

  private AdminAlcoholAttributeSummaryItem toSummary(
      SummaryGroupKey key, List<AlcoholAttribute> rows, long selectedCount) {
    boolean numeric = key.datatype().isNumeric();
    BigDecimal min = null;
    BigDecimal max = null;
    if (numeric) {
      List<BigDecimal> numbers =
          rows.stream().map(row -> new BigDecimal(row.getAttributeValue())).toList();
      min = numbers.stream().min(BigDecimal::compareTo).orElseThrow();
      max = numbers.stream().max(BigDecimal::compareTo).orElseThrow();
    }
    // 비숫자 묶음은 그룹 키에 값이 들어 있어 항상 같은 값이다. 숫자는 min·max가 같으면 같은 값이다
    boolean allSame = !numeric || min.compareTo(max) == 0;
    String value = !numeric || allSame ? rows.get(0).getAttributeValue() : null;
    List<Long> alcoholIds =
        rows.stream().map(AlcoholAttribute::getAlcoholId).distinct().sorted().toList();
    List<Long> attributeIds = rows.stream().map(AlcoholAttribute::getId).sorted().toList();
    return new AdminAlcoholAttributeSummaryItem(
        key.key(),
        value,
        key.type(),
        key.datatype(),
        alcoholIds,
        attributeIds,
        alcoholIds.size(),
        selectedCount,
        min,
        max,
        allSame);
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

  private record SummaryGroupKey(
      String key, AttributeType type, AttributeDatatype datatype, String value) {}
}
