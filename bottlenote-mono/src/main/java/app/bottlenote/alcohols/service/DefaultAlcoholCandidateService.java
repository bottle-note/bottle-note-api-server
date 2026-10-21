package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.candidate.AlcoholCandidateQuery;
import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest.*;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse.*;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.pagination.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAlcoholCandidateService implements AlcoholCandidateService {
  private final AlcoholCandidateRepository repository;
  private final TastingTagGroupRepository tastingGroupRepository;
  private final HmacCursorCodec cursorCodec;
  private final ObjectMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public KeysetPageResponse<AlcoholCandidateResponse> findCandidates(
      Long groupId, AlcoholCandidateRequest request) {
    var group =
        repository
            .findGroup(groupId)
            .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND));
    var definitions =
        repository.findAttributes(groupId).stream()
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
    var query = new AlcoholCandidateQuery(groupId, normalized, tags);
    String context = context(groupId, normalized);
    Long afterId =
        normalized.cursor() == null
            ? null
            : CursorKeys.requireLong(cursorCodec.verify(normalized.cursor(), context), "id");
    var page =
        KeysetPagination.fromOverflow(
            repository.findPage(query, afterId, normalized.size() + 1),
            normalized.size(),
            a -> cursorCodec.encode(context, Map.of("id", a.getId().toString())));
    var ids = page.items().stream().map(Alcohol::getId).toList();
    var mappings =
        repository.findMappings(groupId, ids).stream()
            .collect(Collectors.groupingBy(AlcoholAttributeMapping::getAlcoholId));
    var tastingTags =
        repository.findTags(ids).stream()
            .collect(
                Collectors.groupingBy(
                    AlcoholCandidateRepository.TagRow::alcoholId,
                    Collectors.mapping(
                        AlcoholCandidateRepository.TagRow::tag, Collectors.toList())));
    long total = repository.count(query);
    var items =
        page.items().stream()
            .map(
                a ->
                    toItem(
                        a,
                        mappings.getOrDefault(a.getId(), List.of()),
                        tastingTags.getOrDefault(a.getId(), List.of()),
                        definitions,
                        groups))
            .toList();
    var facets =
        normalized.nextQuestion() == null
            ? null
            : AlcoholCandidateFacets.create(query, total, repository, definitions, groups);
    return KeysetPageResponse.of(
        new AlcoholCandidateResponse(group, items, total, facets), page.pagination());
  }

  private AlcoholCandidateRequest normalize(
      AlcoholCandidateRequest r,
      Map<Long, AlcoholAttribute> definitions,
      List<TastingTagGroup> groups) {
    if (r.size() < 1 || r.size() > 100 || r.include().size() > 20 || r.exclude().size() > 20)
      throw invalid();
    if (r.abvFrom() != null
        && (r.abvFrom().signum() < 0 || r.abvFrom().compareTo(new BigDecimal("100")) > 0))
      throw invalid();
    if (r.abvTo() != null
        && (r.abvTo().signum() < 0 || r.abvTo().compareTo(new BigDecimal("100")) > 0))
      throw invalid();
    if (r.abvFrom() != null && r.abvTo() != null && r.abvFrom().compareTo(r.abvTo()) > 0)
      throw invalid();
    for (String code : r.tastingGroupCodes())
      if (groups.stream().noneMatch(g -> g.getCode().equals(code))) throw invalid();
    var next = r.nextQuestion();
    if (next != null) {
      if (next.source() == null
          || (next.source() == Source.ATTRIBUTE) != (next.attributeId() != null)) throw invalid();
      var attribute =
          next.attributeId() == null ? null : requireDefinition(next.attributeId(), definitions);
      if (!next.ranges().isEmpty()
          && next.source() != Source.ABV
          && (attribute == null || !attribute.getDatatype().isNumeric())) throw invalid();
      Set<String> keys = new HashSet<>();
      for (var range : next.ranges()) {
        if (range == null
            || range.key() == null
            || range.key().isBlank()
            || !keys.add(range.key())
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
    return new AlcoholCandidateRequest(
        r.keyword(),
        normalizeConditions(r.include(), definitions),
        normalizeConditions(r.exclude(), definitions),
        r.categoryGroups().stream().distinct().sorted().toList(),
        r.abvFrom() == null ? null : r.abvFrom().stripTrailingZeros(),
        r.abvTo() == null ? null : r.abvTo().stripTrailingZeros(),
        r.tastingGroupCodes().stream().distinct().sorted().toList(),
        next,
        r.cursor(),
        r.size());
  }

  private List<Condition> normalizeConditions(
      List<Condition> conditions, Map<Long, AlcoholAttribute> definitions) {
    return conditions.stream()
        .map(
            c -> {
              if (c == null || c.operator() == null) throw invalid();
              var a = requireDefinition(c.attributeId(), definitions);
              if (!c.operator().requiresValue()) {
                if (c.value() != null) throw invalid();
                return c;
              }
              if (c.value() == null
                  || (c.operator().isNumericComparison() && !a.getDatatype().isNumeric())
                  || (c.operator() == AttributeSearchOperator.CONTAINS
                      && a.getDatatype() != AttributeDatatype.STRING)) throw invalid();
              return new Condition(
                  c.attributeId(),
                  c.operator(),
                  AlcoholAttributeMapping.normalizeValue(a.getDatatype(), c.value()));
            })
        .distinct()
        .sorted(
            Comparator.comparing(Condition::attributeId)
                .thenComparing(c -> c.operator().name())
                .thenComparing(Condition::value, Comparator.nullsFirst(String::compareTo)))
        .toList();
  }

  static AlcoholAttribute requireDefinition(Long id, Map<Long, AlcoholAttribute> definitions) {
    var attribute = definitions.get(id);
    if (attribute == null) throw new AlcoholException(ALCOHOL_ATTRIBUTE_NOT_FOUND);
    return attribute;
  }

  private String context(Long groupId, AlcoholCandidateRequest r) {
    try {
      return "alcohol.candidates:"
          + mapper.writeValueAsString(
              new FilterContext(
                  groupId,
                  r.keyword(),
                  r.include(),
                  r.exclude(),
                  r.categoryGroups(),
                  r.abvFrom(),
                  r.abvTo(),
                  r.tastingGroupCodes()));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot serialize candidate filter context", e);
    }
  }

  private record FilterContext(
      Long groupId,
      String keyword,
      List<Condition> include,
      List<Condition> exclude,
      List<AlcoholCategoryGroup> categoryGroups,
      BigDecimal abvFrom,
      BigDecimal abvTo,
      List<String> tastingGroupCodes) {}

  private static AlcoholException invalid() {
    return new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
  }

  static BigDecimal parseAbv(String raw) {
    if (raw == null || !raw.trim().matches("[0-9]+(?:[.][0-9]+)?\\s*%?")) return null;
    var number = new BigDecimal(raw.trim().replace("%", "").trim());
    return number.compareTo(new BigDecimal("100")) <= 0 ? number.stripTrailingZeros() : null;
  }

  private CandidateItem toItem(
      Alcohol a,
      List<AlcoholAttributeMapping> mappings,
      List<CandidateTag> tags,
      Map<Long, AlcoholAttribute> definitions,
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
                e -> {
                  var d = definitions.get(e.getKey());
                  return new CandidateAttributeValues(
                      d.getId(),
                      d.getAttributeKey(),
                      d.getDisplayName(),
                      d.getAttributeType(),
                      d.getDatatype(),
                      d.getUnit(),
                      d.isAllowMultipleValues(),
                      d.getAllowedValues(),
                      e.getValue().stream().distinct().toList());
                })
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
}
