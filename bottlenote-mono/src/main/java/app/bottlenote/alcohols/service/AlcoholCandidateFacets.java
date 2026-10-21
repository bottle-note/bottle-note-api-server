package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.AlcoholCandidateRepository.FacetRow;
import app.bottlenote.alcohols.dto.candidate.AlcoholCandidateQuery;
import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest.Source;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class AlcoholCandidateFacets {
  private AlcoholCandidateFacets() {}

  static CandidateFacets create(
      AlcoholCandidateQuery query,
      long total,
      AlcoholCandidateRepository repository,
      Map<Long, AlcoholAttribute> definitions,
      List<TastingTagGroup> groups) {
    var next = query.request().nextQuestion();
    var rows = repository.findFacetRows(query);
    List<CandidateOption> options = new ArrayList<>();
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
        var n =
            next.source() == Source.ABV
                ? DefaultAlcoholCandidateService.parseAbv(row.value())
                : new BigDecimal(row.value());
        if (n == null) continue;
        numbers.computeIfAbsent(row.alcoholId(), ignored -> new ArrayList<>()).add(n);
      }
      present.add(row.alcoholId());
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
      Set<String> values =
          new TreeSet<>(repository.findAttributeOptions(query.groupId(), next.attributeId(), 101));
      if (definition.getAllowedValues() != null) values.addAll(definition.getAllowedValues());
      more = values.size() > 100;
      for (String value : values.stream().limit(100).toList())
        options.add(
            new CandidateOption(
                value, value, null, null, count(rows, row -> value.equals(row.value()))));
    } else if (next.source() == Source.CATEGORY) {
      for (var category : AlcoholCategoryGroup.values())
        options.add(
            new CandidateOption(
                category.name(),
                category.getDescription(),
                null,
                null,
                count(rows, row -> category.name().equals(row.value()))));
    } else if (next.source() == Source.TASTING_GROUP) {
      present.clear();
      for (var group : groups) {
        var ids =
            group.getTags().stream()
                .filter(TastingTagGroupTag::isCore)
                .map(t -> t.getTastingTagId().toString())
                .collect(Collectors.toSet());
        var matched =
            rows.stream()
                .filter(row -> ids.contains(row.value()))
                .map(FacetRow::alcoholId)
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

  private static long count(List<FacetRow> rows, Predicate<FacetRow> predicate) {
    return rows.stream().filter(predicate).map(FacetRow::alcoholId).distinct().count();
  }
}
