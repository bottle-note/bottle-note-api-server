package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.*;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeConditionRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.*;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeFacetsResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.*;
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholAttributeFacetCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.pagination.OffsetPage;
import java.math.BigDecimal;
import java.util.*;
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
  private final AlcoholCandidateRepository candidateRepository;
  private final TastingTagGroupRepository tastingGroupRepository;

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
  public AdminAlcoholAttributeFacetsResponse facets(AdminAlcoholCompoundSearchRequest request) {
    var search = normalize(request);
    var rows = attributeRepository.findFacets(search.request(), search.ids());
    var definitions =
        attributeRepository
            .findAllByIdIn(
                rows.stream().map(AlcoholAttributeFacetCountItem::attributeId).distinct().toList())
            .stream()
            .collect(
                Collectors.toMap(AlcoholAttribute::getId, AlcoholAttributeResponseMapper::toItem));
    return new AdminAlcoholAttributeFacetsResponse(
        rows.stream()
            .limit(request.size())
            .map(
                row ->
                    new AdminAlcoholAttributeFacetsResponse.AdminAlcoholAttributeFacetItem(
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
                  return new AdminAlcoholAttributeConditionRequest(
                      condition.key(),
                      condition.operator(),
                      definition.normalizeSearchValue(condition.operator(), condition.value()));
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

  @Override
  @Transactional(readOnly = true)
  public AdminAlcoholCandidateResponse findCandidates(
      Long groupId, AdminAlcoholCandidateRequest request) {
    var group =
        candidateRepository
            .findGroup(groupId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND));
    var definitions =
        candidateRepository.findAttributes(groupId).stream()
            .collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity()));
    var groups = tastingGroupRepository.findAllWithTags();
    var normalized = normalize(request, definitions, groups);
    var tags =
        groups.stream()
            .filter(g -> normalized.tastingGroupCodes().contains(g.getCode()))
            .flatMap(g -> g.getTags().stream())
            .filter(TastingTagGroupTag::isCore)
            .map(TastingTagGroupTag::getTastingTagId)
            .distinct()
            .toList();
    var query = new AlcoholCandidateCriteria(groupId, normalized, tags);
    var page = candidateRepository.findPage(query);
    var ids = page.stream().map(Alcohol::getId).toList();
    var mappings =
        candidateRepository.findMappings(groupId, ids).stream()
            .collect(Collectors.groupingBy(AlcoholAttributeMapping::getAlcoholId));
    var tastingTags =
        candidateRepository.findTags(ids).stream()
            .collect(
                Collectors.groupingBy(
                    AlcoholCandidateRepository.TagRow::alcoholId,
                    Collectors.mapping(
                        AlcoholCandidateRepository.TagRow::tag, Collectors.toList())));
    long total = candidateRepository.count(query);
    var items =
        page.stream()
            .map(
                a ->
                    toItem(
                        a,
                        mappings.getOrDefault(a.getId(), List.of()),
                        tastingTags.getOrDefault(a.getId(), List.of()),
                        groups))
            .toList();
    var facets =
        normalized.nextQuestion() == null
            ? null
            : candidateFacets(query, total, definitions, groups);
    return new AdminAlcoholCandidateResponse(
        group,
        definitions.values().stream()
            .sorted(Comparator.comparing(AlcoholAttribute::getId))
            .map(
                d ->
                    new CandidateAttributeDefinition(
                        d.getId(),
                        d.getAttributeKey(),
                        d.getDisplayName(),
                        d.getAttributeType(),
                        d.getDatatype(),
                        d.getUnit(),
                        d.isAllowMultipleValues(),
                        d.getAllowedValues()))
            .toList(),
        items,
        total,
        facets);
  }

  private AdminAlcoholCandidateRequest normalize(
      AdminAlcoholCandidateRequest r,
      Map<Long, AlcoholAttribute> definitions,
      List<TastingTagGroup> groups) {
    if (r.abvFrom() != null && r.abvTo() != null && r.abvFrom().compareTo(r.abvTo()) > 0)
      throw invalid();
    for (String code : r.tastingGroupCodes())
      if (groups.stream().noneMatch(g -> g.getCode().equals(code))) throw invalid();
    var next = r.nextQuestion();
    if (next != null) {
      if ((next.source() == Source.ATTRIBUTE) != (next.attributeId() != null)) throw invalid();
      var attribute =
          next.attributeId() == null ? null : requireDefinition(next.attributeId(), definitions);
      if (!next.ranges().isEmpty()
          && next.source() != Source.ABV
          && (attribute == null || !attribute.getDatatype().isNumeric())) throw invalid();
      Set<String> keys = new HashSet<>();
      for (var range : next.ranges()) {
        if (!keys.add(range.key())
            || (range.from() == null && range.to() == null)
            || (range.from() != null
                && range.to() != null
                && range.from().compareTo(range.to()) > 0)) throw invalid();
        if (next.source() == Source.ABV
            && ((range.from() != null
                    && (range.from().signum() < 0
                        || range.from().compareTo(new BigDecimal("100")) > 0))
                || (range.to() != null
                    && (range.to().signum() < 0
                        || range.to().compareTo(new BigDecimal("100")) > 0)))) throw invalid();
      }
    }
    return new AdminAlcoholCandidateRequest(
        r.keyword(),
        normalizeConditions(r.include(), definitions),
        normalizeConditions(r.exclude(), definitions),
        r.categoryGroups(),
        r.abvFrom(),
        r.abvTo(),
        r.tastingGroupCodes(),
        next,
        r.page(),
        r.size());
  }

  private List<Condition> normalizeConditions(
      List<Condition> conditions, Map<Long, AlcoholAttribute> definitions) {
    return conditions.stream()
        .map(
            c -> {
              var attribute = requireDefinition(c.attributeId(), definitions);
              return new Condition(
                  c.attributeId(),
                  c.operator(),
                  attribute.normalizeSearchValue(c.operator(), c.value()));
            })
        .toList();
  }

  private static AlcoholAttribute requireDefinition(
      Long id, Map<Long, AlcoholAttribute> definitions) {
    var attribute = definitions.get(id);
    if (attribute == null) throw new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND);
    return attribute;
  }

  private static AlcoholException invalid() {
    return new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
  }

  private static BigDecimal parseAbv(String raw) {
    if (raw == null || !raw.trim().matches("[0-9]+(?:[.][0-9]+)?\\s*%?")) return null;
    var number = new BigDecimal(raw.trim().replace("%", "").trim());
    return number.compareTo(new BigDecimal("100")) <= 0 ? number.stripTrailingZeros() : null;
  }

  private CandidateItem toItem(
      Alcohol a,
      List<AlcoholAttributeMapping> mappings,
      List<CandidateTag> tags,
      List<TastingTagGroup> groups) {
    var values =
        mappings.stream()
            .collect(
                Collectors.groupingBy(
                    AlcoholAttributeMapping::getAttributeId,
                    Collectors.mapping(
                        AlcoholAttributeMapping::getAttributeValue, Collectors.toList())));
    var attrs =
        values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                e ->
                    new CandidateAttributeValues(
                        e.getKey(), e.getValue().stream().distinct().toList()))
            .toList();
    Set<Long> tagIds = tags.stream().map(CandidateTag::id).collect(Collectors.toSet());
    var groupCodes =
        groups.stream()
            .filter(
                g ->
                    g.getTags().stream()
                        .anyMatch(t -> t.isCore() && tagIds.contains(t.getTastingTagId())))
            .map(TastingTagGroup::getCode)
            .toList();
    var region = a.getRegion();
    var distillery = a.getDistillery();
    return new CandidateItem(
        a.getId(),
        a.getKorName(),
        a.getEngName(),
        a.getImageUrl(),
        a.getType(),
        a.getCategoryGroup(),
        a.getKorCategory(),
        a.getEngCategory(),
        a.getAbv(),
        parseAbv(a.getAbv()),
        a.getAge(),
        a.getCask(),
        a.getVolume(),
        region == null
            ? null
            : new CandidateNamedItem(region.getId(), region.getKorName(), region.getEngName()),
        distillery == null
            ? null
            : new CandidateNamedItem(
                distillery.getId(), distillery.getKorName(), distillery.getEngName()),
        tags,
        groupCodes,
        attrs);
  }

  private CandidateFacets candidateFacets(
      AlcoholCandidateCriteria query,
      long total,
      Map<Long, AlcoholAttribute> definitions,
      List<TastingTagGroup> groups) {
    var next = query.request().nextQuestion();
    var rows = candidateRepository.findFacetRows(query);
    List<CandidateOption> options = new ArrayList<>();
    Map<String, Set<Long>> matches = new TreeMap<>();
    Map<Long, List<BigDecimal>> numbers = new HashMap<>();
    Set<Long> present = new HashSet<>();
    boolean more = false;
    boolean numeric =
        next.source() == Source.ABV
            || (next.source() == Source.ATTRIBUTE
                && definitions.get(next.attributeId()).getDatatype().isNumeric());
    for (var row : rows) {
      if (row.value() == null) continue;
      if (numeric) {
        var n = next.source() == Source.ABV ? parseAbv(row.value()) : new BigDecimal(row.value());
        if (n == null) continue;
        numbers.computeIfAbsent(row.alcoholId(), ignored -> new ArrayList<>()).add(n);
      }
      present.add(row.alcoholId());
      matches.computeIfAbsent(row.value(), ignored -> new HashSet<>()).add(row.alcoholId());
    }
    if (!next.ranges().isEmpty()) {
      for (var range : next.ranges()) {
        long count =
            numbers.values().stream()
                .filter(
                    values ->
                        values.stream()
                            .anyMatch(
                                n ->
                                    (range.from() == null || n.compareTo(range.from()) >= 0)
                                        && (range.to() == null || n.compareTo(range.to()) <= 0)))
                .count();
        options.add(new CandidateOption(range.key(), range.key(), range.from(), range.to(), count));
      }
    } else if (next.source() == Source.ATTRIBUTE) {
      var definition = definitions.get(next.attributeId());
      Set<String> values = new LinkedHashSet<>(matches.keySet());
      Set<String> otherValues =
          new TreeSet<>(
              candidateRepository.findAttributeOptions(query.groupId(), next.attributeId(), 101));
      if (definition.getAllowedValues() != null) otherValues.addAll(definition.getAllowedValues());
      values.addAll(otherValues);
      more = values.size() > 100;
      for (String value : values.stream().limit(100).toList())
        options.add(
            new CandidateOption(
                value, value, null, null, matches.getOrDefault(value, Set.of()).size()));
    } else if (next.source() == Source.CATEGORY) {
      for (var category : AlcoholCategoryGroup.values())
        options.add(
            new CandidateOption(
                category.name(),
                category.getDescription(),
                null,
                null,
                matches.getOrDefault(category.name(), Set.of()).size()));
    } else if (next.source() == Source.TASTING_GROUP) {
      present.clear();
      for (var group : groups) {
        var matched =
            group.getTags().stream()
                .filter(TastingTagGroupTag::isCore)
                .flatMap(
                    t -> matches.getOrDefault(t.getTastingTagId().toString(), Set.of()).stream())
                .collect(Collectors.toSet());
        present.addAll(matched);
        options.add(
            new CandidateOption(group.getCode(), group.getName(), null, null, matched.size()));
      }
    }
    var values = numbers.values().stream().flatMap(Collection::stream).toList();
    return new CandidateFacets(
        next.source(),
        next.attributeId(),
        List.copyOf(options),
        more,
        total - present.size(),
        values.stream().min(BigDecimal::compareTo).orElse(null),
        values.stream().max(BigDecimal::compareTo).orElse(null));
  }
}
