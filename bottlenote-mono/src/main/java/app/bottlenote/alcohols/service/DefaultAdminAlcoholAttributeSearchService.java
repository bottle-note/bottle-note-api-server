package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholAttributeMapping;
import app.bottlenote.alcohols.domain.AlcoholAttributeMappingRepository;
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeConditionRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeFacets;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeFacetCount;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.pagination.OffsetPage;
import java.util.Comparator;
import java.util.List;
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
public class DefaultAdminAlcoholAttributeSearchService
    implements AdminAlcoholAttributeSearchService {
  private final AlcoholAttributeRepository attributeRepository;
  private final AlcoholAttributeMappingRepository mappingRepository;
  private final AlcoholQueryRepository alcoholQueryRepository;

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
              request.size(),
              request.groupIds());
    }
    var page = attributeRepository.findAlcoholIds(request);
    return toAlcoholPage(page, request.page(), request.size());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAttributedAlcoholItem> searchAlcohols(
      AdminAlcoholCompoundSearchRequest request) {
    var search = normalize(request);
    return toAlcoholPage(
        attributeRepository.findAlcoholIds(search.request(), search.ids()),
        request.page(),
        request.size());
  }

  @Override
  @Transactional(readOnly = true)
  public AdminAlcoholAttributeFacets facets(AdminAlcoholCompoundSearchRequest request) {
    var search = normalize(request);
    var rows = attributeRepository.findFacets(search.request(), search.ids());
    var definitions =
        attributeRepository
            .findAllByIdIn(
                rows.stream().map(AlcoholAttributeFacetCount::attributeId).distinct().toList())
            .stream()
            .collect(
                Collectors.toMap(AlcoholAttribute::getId, AlcoholAttributeResponseMapper::toItem));
    return new AdminAlcoholAttributeFacets(
        rows.stream()
            .limit(request.size())
            .map(
                row ->
                    new AdminAlcoholAttributeFacets.AdminAlcoholAttributeFacetItem(
                        definitions.get(row.attributeId()), row.value(), row.alcoholCount()))
            .toList(),
        rows.size() > request.size());
  }

  private record NormalizedSearch(
      AdminAlcoholCompoundSearchRequest request, java.util.Map<String, Long> ids) {}

  private NormalizedSearch normalize(AdminAlcoholCompoundSearchRequest request) {
    if (request.conditions().size() > 20)
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    for (var condition : request.conditions()) {
      if (condition == null
          || condition.key() == null
          || !condition.key().matches("[a-z][a-z0-9_]{0,63}"))
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    var keys =
        request.conditions().stream()
            .map(AdminAlcoholAttributeConditionRequest::key)
            .distinct()
            .toList();
    var definitions =
        keys.isEmpty()
            ? java.util.Map.<String, AlcoholAttribute>of()
            : attributeRepository.findAllByAttributeKeyIn(keys).stream()
                .collect(Collectors.toMap(AlcoholAttribute::getAttributeKey, Function.identity()));
    var conditions =
        request.conditions().stream()
            .map(
                condition -> {
                  var definition = definitions.get(condition.key());
                  if (definition == null) throw new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND);
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
                  return new AdminAlcoholAttributeConditionRequest(
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
            request.size(),
            request.groupIds());
    return new NormalizedSearch(
        normalized,
        definitions.entrySet().stream()
            .collect(
                Collectors.toMap(java.util.Map.Entry::getKey, entry -> entry.getValue().getId())));
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
            .collect(
                Collectors.toMap(AlcoholAttribute::getId, AlcoholAttributeResponseMapper::toItem));
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
}
