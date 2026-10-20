package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.constant.ValueProvider;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingKey;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCondition;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSummaryRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import app.bottlenote.alcohols.dto.response.AdminAuditPrincipalItem;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminAlcoholAttributeService implements AdminAlcoholAttributeService {
  private final AlcoholAttributeRepository attributeRepository;
  private final AlcoholAttributeMappingRepository mappingRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAlcoholAttributeItem> findAll(AdminAlcoholAttributeSearchRequest request) {
    var page = attributeRepository.search(request);
    return new PageImpl<>(
        page.content().stream().map(this::toItem).toList(),
        PageRequest.of(request.page(), request.size()),
        page.totalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeSummaryItem> summarize(
      AdminAlcoholAttributeSummaryRequest request) {
    if (request.alcoholIds().isEmpty()) return List.of();
    var mappings = mappingRepository.findAllByAlcoholIdIn(request.alcoholIds());
    var grouped =
        mappings.stream().collect(Collectors.groupingBy(AlcoholAttributeMapping::getAttributeId));
    if (grouped.isEmpty()) return List.of();
    long selectedCount = request.alcoholIds().stream().distinct().count();
    return attributeRepository.findAllByIdIn(grouped.keySet()).stream()
        .sorted(Comparator.comparing(AlcoholAttribute::getId))
        .map(
            a -> {
              var rows = grouped.get(a.getId());
              var alcoholIds =
                  rows.stream()
                      .map(AlcoholAttributeMapping::getAlcoholId)
                      .distinct()
                      .sorted()
                      .toList();
              return new AdminAlcoholAttributeSummaryItem(
                  toItem(a),
                  alcoholIds,
                  rows.stream().map(AlcoholAttributeMapping::getId).sorted().toList(),
                  rows.stream().map(this::toMappingItem).toList(),
                  alcoholIds.size(),
                  selectedCount);
            })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAttributedAlcoholItem> searchAlcohols(
      AdminAttributedAlcoholSearchRequest request) {
    if (request.operator() == AttributeOperator.GTE
        && (request.key() == null || request.value() == null))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    if (request.key() != null && request.value() != null) {
      var definition =
          attributeRepository
              .findByAttributeKey(request.key())
              .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND));
      if (request.operator() == AttributeOperator.GTE && !definition.getDatatype().isNumeric())
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
      request =
          new AdminAttributedAlcoholSearchRequest(
              request.keyword(),
              request.type(),
              request.key(),
              AlcoholAttributeMapping.normalizeValue(definition.getDatatype(), request.value()),
              request.operator(),
              request.missingKey(),
              request.page(),
              request.size());
    }
    var page = attributeRepository.findAlcoholIds(request);
    return toAlcoholPage(page, request.page(), request.size());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAttributedAlcoholItem> searchAlcohols(
      AdminAlcoholCompoundSearchRequest request) {
    if (request.conditions().size() > 20)
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    var definitions = new java.util.HashMap<String, AlcoholAttribute>();
    var conditions =
        request.conditions().stream()
            .map(
                condition -> {
                  if (condition == null
                      || condition.key() == null
                      || !condition.key().matches("[a-z][a-z0-9_]{0,63}"))
                    throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
                  var definition =
                      definitions.computeIfAbsent(
                          condition.key(),
                          key ->
                              attributeRepository
                                  .findByAttributeKey(key)
                                  .orElseThrow(
                                      () -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND)));
                  var operator = condition.operator();
                  if (!operator.requiresValue()) {
                    if (condition.value() != null)
                      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
                    return condition;
                  }
                  if (condition.value() == null
                      || (operator.isNumericComparison() && !definition.getDatatype().isNumeric())
                      || (operator == AttributeSearchOperator.CONTAINS
                          && definition.getDatatype() != AttributeDatatype.STRING))
                    throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
                  return new AdminAlcoholAttributeCondition(
                      condition.key(),
                      operator,
                      AlcoholAttributeMapping.normalizeValue(
                          definition.getDatatype(), condition.value()));
                })
            .toList();
    var normalized =
        new AdminAlcoholCompoundSearchRequest(
            request.keyword(),
            request.match(),
            conditions,
            request.type(),
            request.linkedOnly(),
            request.page(),
            request.size());
    return toAlcoholPage(
        attributeRepository.findAlcoholIds(normalized), request.page(), request.size());
  }

  private Page<AdminAttributedAlcoholItem> toAlcoholPage(
      OffsetPage<Long> page, int pageNumber, int size) {
    var pageable = PageRequest.of(pageNumber, size);
    if (page.content().isEmpty()) return new PageImpl<>(List.of(), pageable, page.totalElements());
    var alcohols =
        alcoholQueryRepository.findAllByIdIn(page.content()).stream()
            .collect(Collectors.toMap(Alcohol::getId, Function.identity()));
    var mappings = mappingRepository.findAllByAlcoholIdIn(page.content());
    var attributes =
        attributeRepository
            .findAllByIdIn(
                mappings.stream().map(AlcoholAttributeMapping::getAttributeId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(AlcoholAttribute::getId, this::toItem));
    var grouped =
        mappings.stream().collect(Collectors.groupingBy(AlcoholAttributeMapping::getAlcoholId));
    var content =
        page.content().stream()
            .map(
                id -> {
                  Alcohol a = alcohols.get(id);
                  return new AdminAttributedAlcoholItem(
                      id,
                      a.getKorName(),
                      a.getEngName(),
                      a.getKorCategory(),
                      a.getImageUrl(),
                      grouped.getOrDefault(id, List.of()).stream()
                          .filter(m -> attributes.containsKey(m.getAttributeId()))
                          .sorted(Comparator.comparing(AlcoholAttributeMapping::getId))
                          .map(
                              m ->
                                  new AdminAttributedAlcoholItem.AttributeValue(
                                      m.getId(),
                                      m.getAttributeValue(),
                                      attributes.get(m.getAttributeId())))
                          .toList());
                })
            .toList();
    return new PageImpl<>(content, pageable, page.totalElements());
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeItem create(AdminAlcoholAttributeCreateRequest request) {
    var result = upsertBulk(new AdminAlcoholAttributeBulkRequest(List.of(request)));
    return toItem(requireAttribute(result.ids().getFirst()));
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeBulkResponse upsertBulk(AdminAlcoholAttributeBulkRequest request) {
    var candidates = request.items().stream().map(this::candidate).toList();
    Set<String> keys = new HashSet<>();
    for (var candidate : candidates) {
      if (!keys.add(candidate.getAttributeKey()))
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_DUPLICATE_KEY_IN_REQUEST);
    }
    var existingAttributes =
        attributeRepository.lockByKeys(keys).stream()
            .collect(Collectors.toMap(AlcoholAttribute::getAttributeKey, Function.identity()));
    int created = 0;
    int updated = 0;
    List<Long> ids = new ArrayList<>();
    for (var candidate : candidates) {
      var existing = Optional.ofNullable(existingAttributes.get(candidate.getAttributeKey()));
      Long id = existing.map(AlcoholAttribute::getId).orElse(null);
      requireUniqueName(candidate.getDisplayName(), id);
      AlcoholAttribute target;
      if (existing.isPresent()) {
        target = existing.get();
        requireCompatibleDatatype(target, candidate);
        target.updateFrom(candidate);
        updated++;
      } else {
        target = candidate;
        created++;
      }
      ids.add(attributeRepository.save(target).getId());
    }
    return new AdminAlcoholAttributeBulkResponse(created, updated, List.copyOf(ids));
  }

  @Override
  @Transactional
  public AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request) {
    var target = requireLockedAttribute(id);
    var candidate =
        AlcoholAttribute.create(
            target.getAttributeKey(),
            request.displayName(),
            request.type(),
            request.datatype(),
            request.unit(),
            request.description());
    requireUniqueName(candidate.getDisplayName(), id);
    requireCompatibleDatatype(target, candidate);
    target.updateFrom(candidate);
    attributeRepository.save(target);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_UPDATED, id);
  }

  @Override
  @Transactional
  public AdminResultResponse delete(Long id) {
    var target = requireLockedAttribute(id);
    mappingRepository.deleteAllByAttributeIdIn(List.of(id));
    attributeRepository.delete(target);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_DELETED, id);
  }

  @Override
  @Transactional
  public List<AdminAlcoholAttributeItem> deleteBatch(
      AdminAlcoholAttributeBatchDeleteRequest request) {
    var rows = attributeRepository.lockByIds(request.ids());
    var result = rows.stream().map(this::toItem).toList();
    if (!rows.isEmpty()) {
      var ids = rows.stream().map(AlcoholAttribute::getId).toList();
      mappingRepository.deleteAllByAttributeIdIn(ids);
      attributeRepository.deleteAllByIdIn(ids);
    }
    return result;
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeMappingBulkResponse mapBulk(
      AdminAlcoholAttributeMappingBulkRequest request) {
    var alcoholIds =
        request.items().stream()
            .map(AdminAlcoholAttributeMappingBulkRequest.Item::alcoholId)
            .distinct()
            .sorted()
            .toList();
    var attributeIds =
        request.items().stream()
            .map(AdminAlcoholAttributeMappingBulkRequest.Item::attributeId)
            .distinct()
            .sorted()
            .toList();
    var attributes =
        attributeRepository.lockByIds(attributeIds).stream()
            .collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity()));
    if (attributes.size() != attributeIds.size())
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND);
    var alcohols = mappingRepository.lockAlcohols(alcoholIds);
    if (alcohols.size() != alcoholIds.size() || alcohols.stream().anyMatch(Alcohol::isDeleted))
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    var candidates =
        request.items().stream()
            .map(
                item ->
                    AlcoholAttributeMapping.create(
                        item.alcoholId(), attributes.get(item.attributeId()), item.value()))
            .toList();
    Set<AlcoholAttributeMappingKey> existing =
        new HashSet<>(mappingRepository.findExistingKeys(alcoholIds, attributeIds));
    List<Long> ids = new ArrayList<>();
    int skipped = 0;
    for (var item : candidates) {
      if (!existing.add(
          new AlcoholAttributeMappingKey(item.getAlcoholId(), item.getAttributeId()))) {
        skipped++;
        continue;
      }
      ids.add(mappingRepository.save(item).getId());
    }
    return new AdminAlcoholAttributeMappingBulkResponse(ids.size(), skipped, List.copyOf(ids));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAlcoholAttributeMappingItem> findMappings(
      AdminAlcoholAttributeMappingSearchRequest request) {
    var page = mappingRepository.search(request);
    return new PageImpl<>(
        page.content().stream().map(this::toMappingItem).toList(),
        PageRequest.of(request.page(), request.size()),
        page.totalElements());
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeMappingItem updateMapping(
      Long id, AdminAlcoholAttributeMappingUpdateRequest request) {
    var mapping =
        mappingRepository
            .findById(id)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_MAPPING_NOT_FOUND));
    var attribute = requireLockedAttribute(mapping.getAttributeId());
    var alcohols = mappingRepository.lockAlcohols(List.of(mapping.getAlcoholId()));
    if (alcohols.isEmpty() || alcohols.getFirst().isDeleted())
      throw new AlcoholException(ALCOHOL_NOT_FOUND);
    mapping.updateValue(attribute.getDatatype(), request.value());
    return toMappingItem(mappingRepository.save(mapping));
  }

  @Override
  @Transactional
  public AdminResultResponse deleteMapping(Long id) {
    var mapping =
        mappingRepository
            .findById(id)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_MAPPING_NOT_FOUND));
    mappingRepository.delete(mapping);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_MAPPING_DELETED, id);
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
        AdminCodeLabelItem.listOf(ValueProvider.values()),
        AdminCodeLabelItem.listOf(AttributeOperator.values()));
  }

  private AlcoholAttribute candidate(AdminAlcoholAttributeCreateRequest request) {
    return AlcoholAttribute.create(
        request.key(),
        request.displayName(),
        request.type(),
        request.datatype(),
        request.unit(),
        request.description());
  }

  private AlcoholAttribute requireAttribute(Long id) {
    return attributeRepository
        .findById(id)
        .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND));
  }

  private void requireUniqueName(String name, Long id) {
    if (attributeRepository.existsByDisplayNameAndIdNot(name, id))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_DUPLICATE);
  }

  private AdminAlcoholAttributeItem toItem(AlcoholAttribute a) {
    return new AdminAlcoholAttributeItem(
        a.getId(),
        a.getAttributeKey(),
        a.getDisplayName(),
        a.getAttributeType(),
        a.getDatatype(),
        a.getUnit(),
        a.getDescription(),
        a.getCreateAt(),
        a.getLastModifyAt(),
        toPrincipal(a.getCreatePrincipal()),
        toPrincipal(a.getLastModifyPrincipal()));
  }

  private AlcoholAttribute requireLockedAttribute(Long id) {
    return attributeRepository.lockByIds(List.of(id)).stream()
        .findFirst()
        .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND));
  }

  private void requireCompatibleDatatype(AlcoholAttribute target, AlcoholAttribute candidate) {
    if (target.getDatatype() != candidate.getDatatype()
        && mappingRepository.existsByAttributeId(target.getId()))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_DATATYPE_IN_USE);
  }

  private AdminAlcoholAttributeMappingItem toMappingItem(AlcoholAttributeMapping m) {
    return new AdminAlcoholAttributeMappingItem(
        m.getId(),
        m.getAlcoholId(),
        m.getAttributeId(),
        m.getAttributeValue(),
        m.getCreateAt(),
        m.getLastModifyAt(),
        toPrincipal(m.getCreatePrincipal()),
        toPrincipal(m.getLastModifyPrincipal()));
  }

  private AdminAuditPrincipalItem toPrincipal(AuditPrincipal principal) {
    return principal == null
        ? null
        : new AdminAuditPrincipalItem(principal.getId(), principal.getType(), principal.getEmail());
  }
}
