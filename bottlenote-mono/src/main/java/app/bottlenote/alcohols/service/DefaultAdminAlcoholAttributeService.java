package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.constant.ValueProvider;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeGroupRepository;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingKey;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingBulkRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeMappingUpdateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSummaryRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingBulkResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeMappingItem;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeyCountItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeKeywordCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.dto.response.AdminResultResponse;
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
  private final AlcoholAttributeGroupRepository groupRepository;
  private final ScenarioReferenceGuard scenarioReferenceGuard;

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAlcoholAttributeItem> findAll(AdminAlcoholAttributeSearchRequest request) {
    var page = attributeRepository.search(request);
    return new PageImpl<>(
        page.content().stream().map(AlcoholAttributeResponseMapper::toItem).toList(),
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
                  AlcoholAttributeResponseMapper.toItem(a),
                  alcoholIds,
                  rows.stream().map(AlcoholAttributeMapping::getId).sorted().toList(),
                  rows.stream().map(this::toMappingItem).toList(),
                  alcoholIds.size(),
                  selectedCount);
            })
        .toList();
  }

  @Override
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public AdminAlcoholAttributeItem create(AdminAlcoholAttributeCreateRequest request) {
    var result = upsertBulk(new AdminAlcoholAttributeBulkRequest(List.of(request)));
    return AlcoholAttributeResponseMapper.toItem(requireAttribute(result.ids().getFirst()));
  }

  @Override
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public AdminAlcoholAttributeBulkResponse upsertBulk(AdminAlcoholAttributeBulkRequest request) {
    var candidates = request.items().stream().map(this::candidate).toList();
    Set<String> keys = new HashSet<>();
    for (var candidate : candidates) {
      if (!keys.add(candidate.getAttributeKey()))
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_DUPLICATE_KEY_IN_REQUEST);
    }
    requireGroups(
        request.items().stream()
            .map(AdminAlcoholAttributeCreateRequest::groupId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted()
            .toList());
    var requestsByKey =
        request.items().stream()
            .collect(Collectors.toMap(r -> r.key().trim(), Function.identity()));
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
        var item = requestsByKey.get(candidate.getAttributeKey());
        candidate.changeStringRules(
            item.allowedValuesPresent() ? item.allowedValues() : target.getAllowedValues(),
            item.patternPresent() ? item.pattern() : target.getPattern());
        candidate.changeAllowMultipleValues(
            item.allowMultipleValues() != null
                ? item.allowMultipleValues()
                : target.isAllowMultipleValues());
        requireCompatibleMultiplicity(target, candidate);
        requireCompatibleRules(target, candidate);
        scenarioReferenceGuard.requireCompatible(
            target,
            candidate,
            item.groupIdPresent() ? candidate.getGroupId() : target.getGroupId());
        target.updateFrom(candidate);
        if (requestsByKey.get(candidate.getAttributeKey()).groupIdPresent())
          target.changeGroup(candidate.getGroupId());
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
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public AdminResultResponse update(Long id, AdminAlcoholAttributeUpdateRequest request) {
    if (request.groupId() != null) requireGroups(List.of(request.groupId()));
    var target = requireLockedAttribute(id);
    var candidate =
        AlcoholAttribute.create(
            target.getAttributeKey(),
            request.displayName(),
            request.type(),
            request.datatype(),
            request.unit(),
            request.description());
    requireCompatibleDatatype(target, candidate);
    candidate.changeStringRules(
        request.allowedValuesPresent() ? request.allowedValues() : target.getAllowedValues(),
        request.patternPresent() ? request.pattern() : target.getPattern());
    candidate.changeAllowMultipleValues(
        request.allowMultipleValues() != null
            ? request.allowMultipleValues()
            : target.isAllowMultipleValues());
    requireCompatibleMultiplicity(target, candidate);
    requireUniqueName(candidate.getDisplayName(), id);
    requireCompatibleRules(target, candidate);
    scenarioReferenceGuard.requireCompatible(
        target, candidate, request.groupIdPresent() ? request.groupId() : target.getGroupId());
    target.updateFrom(candidate);
    if (request.groupIdPresent()) target.changeGroup(request.groupId());
    attributeRepository.save(target);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_UPDATED, id);
  }

  @Override
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public AdminResultResponse delete(Long id) {
    var target = requireLockedAttribute(id);
    scenarioReferenceGuard.beforeAttributeDeletion(List.of(id));
    mappingRepository.deleteAllByAttributeIdIn(List.of(id));
    attributeRepository.delete(target);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_DELETED, id);
  }

  @Override
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public List<AdminAlcoholAttributeItem> deleteBatch(
      AdminAlcoholAttributeBatchDeleteRequest request) {
    var rows = attributeRepository.lockByIds(request.ids());
    var result = rows.stream().map(AlcoholAttributeResponseMapper::toItem).toList();
    if (!rows.isEmpty()) {
      var ids = rows.stream().map(AlcoholAttribute::getId).toList();
      scenarioReferenceGuard.beforeAttributeDeletion(ids);
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
      boolean duplicate =
          attributes.get(item.getAttributeId()).isAllowMultipleValues()
              ? mappingRepository.existsEquivalentMapping(
                  item.getAlcoholId(), item.getAttributeId(), item.getAttributeValue(), null)
              : !existing.add(
                  new AlcoholAttributeMappingKey(item.getAlcoholId(), item.getAttributeId()));
      if (duplicate) {
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
  // 잠금 대기 전에 읽은 매핑이 있어도 중복 검사는 최신 커밋을 기준으로 한다.
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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
    String value = AlcoholAttributeMapping.normalizeValue(attribute.getDatatype(), request.value());
    attribute.validateValue(value);
    if (mappingRepository.existsEquivalentMapping(
        mapping.getAlcoholId(), attribute.getId(), value, id))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_MAPPING_DUPLICATE);
    mapping.updateValue(attribute, value);
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
    var candidate =
        AlcoholAttribute.create(
            request.key(),
            request.displayName(),
            request.type(),
            request.datatype(),
            request.unit(),
            request.description());
    candidate.changeAllowMultipleValues(Boolean.TRUE.equals(request.allowMultipleValues()));
    candidate.changeStringRules(request.allowedValues(), request.pattern());
    if (request.groupIdPresent()) candidate.changeGroup(request.groupId());
    return candidate;
  }

  private void requireGroups(List<Long> ids) {
    if (ids.isEmpty()) return;
    if (ids.stream().anyMatch(id -> id <= 0))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE);
    if (groupRepository.lockByIds(ids).size() != ids.size())
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND);
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

  private void requireCompatibleMultiplicity(AlcoholAttribute target, AlcoholAttribute candidate) {
    if (target.isAllowMultipleValues()
        && !candidate.isAllowMultipleValues()
        && mappingRepository.hasMultipleValues(target.getId()))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_MULTIPLE_VALUES_IN_USE);
  }

  private void requireCompatibleRules(AlcoholAttribute target, AlcoholAttribute candidate) {
    if ((java.util.Objects.equals(target.getAllowedValues(), candidate.getAllowedValues())
            && java.util.Objects.equals(target.getPattern(), candidate.getPattern()))
        || (candidate.getAllowedValues() == null && candidate.getPattern() == null)) return;
    for (String value : mappingRepository.findValuesByAttributeId(target.getId())) {
      try {
        candidate.validateValue(value);
      } catch (AlcoholException e) {
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_RULE_IN_USE);
      }
    }
  }

  private AdminAlcoholAttributeMappingItem toMappingItem(AlcoholAttributeMapping m) {
    return new AdminAlcoholAttributeMappingItem(
        m.getId(),
        m.getAlcoholId(),
        m.getAttributeId(),
        m.getAttributeValue(),
        m.getCreateAt(),
        m.getLastModifyAt(),
        AlcoholAttributeResponseMapper.toPrincipal(m.getCreatePrincipal()),
        AlcoholAttributeResponseMapper.toPrincipal(m.getLastModifyPrincipal()));
  }
}
