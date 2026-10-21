package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholAttribute.alcoholAttribute;
import static app.bottlenote.alcohols.domain.QAlcoholAttributeGroup.alcoholAttributeGroup;
import static app.bottlenote.alcohols.domain.QAlcoholAttributeMapping.alcoholAttributeMapping;
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.alcohols.domain.QTastingTag.tastingTag;

import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.Condition;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.CandidateGroup;
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse.CandidateTag;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class QueryDslAlcoholCandidateRepository implements AlcoholCandidateRepository {
  private final JPAQueryFactory factory;

  @Override
  public Optional<CandidateGroup> findGroup(Long id) {
    return Optional.ofNullable(
        factory
            .select(
                Projections.constructor(
                    CandidateGroup.class, alcoholAttributeGroup.id, alcoholAttributeGroup.name))
            .from(alcoholAttributeGroup)
            .where(alcoholAttributeGroup.id.eq(id))
            .fetchOne());
  }

  @Override
  public List<AlcoholAttribute> findAttributes(Long groupId) {
    return factory
        .selectFrom(alcoholAttribute)
        .where(alcoholAttribute.groupId.eq(groupId))
        .orderBy(alcoholAttribute.id.asc())
        .fetch();
  }

  @Override
  public long count(AlcoholCandidateCriteria query) {
    Long count =
        factory.select(alcohol.id.count()).from(alcohol).where(predicate(query)).fetchOne();
    return count == null ? 0 : count;
  }

  @Override
  public List<Alcohol> findPage(AlcoholCandidateCriteria query) {
    return factory
        .selectFrom(alcohol)
        .leftJoin(alcohol.region)
        .fetchJoin()
        .leftJoin(alcohol.distillery)
        .fetchJoin()
        .where(predicate(query))
        .orderBy(alcohol.id.asc())
        .offset((long) query.request().page() * query.request().size())
        .limit(query.request().size())
        .fetch();
  }

  @Override
  public List<AlcoholAttributeMapping> findMappings(Long groupId, List<Long> ids) {
    if (ids.isEmpty()) return List.of();
    return factory
        .selectFrom(alcoholAttributeMapping)
        .join(alcoholAttribute)
        .on(alcoholAttribute.id.eq(alcoholAttributeMapping.attributeId))
        .where(alcoholAttribute.groupId.eq(groupId), alcoholAttributeMapping.alcoholId.in(ids))
        .orderBy(alcoholAttributeMapping.id.asc())
        .fetch();
  }

  @Override
  public List<TagRow> findTags(List<Long> ids) {
    if (ids.isEmpty()) return List.of();
    return factory
        .select(
            alcoholsTastingTags.alcohol.id,
            tastingTag.id,
            tastingTag.korName,
            tastingTag.engName,
            tastingTag.parentId)
        .from(alcoholsTastingTags)
        .join(alcoholsTastingTags.tastingTag, tastingTag)
        .where(alcoholsTastingTags.alcohol.id.in(ids))
        .orderBy(tastingTag.id.asc())
        .fetch()
        .stream()
        .map(
            t ->
                new TagRow(
                    t.get(alcoholsTastingTags.alcohol.id),
                    new CandidateTag(
                        t.get(tastingTag.id),
                        t.get(tastingTag.korName),
                        t.get(tastingTag.engName),
                        t.get(tastingTag.parentId))))
        .toList();
  }

  @Override
  public List<String> findAttributeOptions(Long groupId, Long attributeId, int limit) {
    return factory
        .select(alcoholAttributeMapping.attributeValue)
        .distinct()
        .from(alcoholAttributeMapping)
        .join(alcohol)
        .on(alcohol.id.eq(alcoholAttributeMapping.alcoholId))
        .where(scope(groupId), alcoholAttributeMapping.attributeId.eq(attributeId))
        .orderBy(alcoholAttributeMapping.attributeValue.asc())
        .limit(limit)
        .fetch();
  }

  @Override
  public List<FacetRow> findFacetRows(AlcoholCandidateCriteria query) {
    var next = query.request().nextQuestion();
    var where = predicate(query);
    if (next.source()
        == app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.Source.ATTRIBUTE) {
      query.request().include().stream()
          .filter(
              c -> c.attributeId().equals(next.attributeId()) && c.operator().isNumericComparison())
          .forEach(c -> where.and(numericValue(alcoholAttributeMapping, c)));
    }
    return switch (next.source()) {
      case ATTRIBUTE ->
          factory
              .select(
                  Projections.constructor(
                      FacetRow.class, alcohol.id, alcoholAttributeMapping.attributeValue))
              .from(alcohol)
              .join(alcoholAttributeMapping)
              .on(alcoholAttributeMapping.alcoholId.eq(alcohol.id))
              .where(where, alcoholAttributeMapping.attributeId.eq(next.attributeId()))
              .fetch();
      case CATEGORY ->
          factory
              .select(
                  Projections.constructor(
                      FacetRow.class, alcohol.id, alcohol.categoryGroup.stringValue()))
              .from(alcohol)
              .where(where)
              .fetch();
      case ABV ->
          factory
              .select(Projections.constructor(FacetRow.class, alcohol.id, alcohol.abv))
              .from(alcohol)
              .where(where)
              .fetch();
      case TASTING_GROUP ->
          factory
              .select(
                  Projections.constructor(
                      FacetRow.class, alcohol.id, alcoholsTastingTags.tastingTag.id.stringValue()))
              .from(alcohol)
              .join(alcohol.alcoholsTastingTags, alcoholsTastingTags)
              .where(where)
              .fetch();
    };
  }

  private BooleanExpression scope(Long groupId) {
    var m = new QAlcoholAttributeMapping("candidateScopeMapping");
    var a = new QAlcoholAttribute("candidateScopeAttribute");
    return alcohol
        .deletedAt
        .isNull()
        .and(
            JPAExpressions.selectOne()
                .from(m)
                .join(a)
                .on(a.id.eq(m.attributeId))
                .where(m.alcoholId.eq(alcohol.id), a.groupId.eq(groupId))
                .exists());
  }

  private BooleanBuilder predicate(AlcoholCandidateCriteria query) {
    var r = query.request();
    var where = new BooleanBuilder(scope(query.groupId()));
    if (r.keyword() != null)
      where.and(
          alcohol
              .korName
              .containsIgnoreCase(r.keyword())
              .or(alcohol.engName.containsIgnoreCase(r.keyword())));
    if (!r.categoryGroups().isEmpty()) where.and(alcohol.categoryGroup.in(r.categoryGroups()));
    for (var c : r.include()) if (!c.operator().isNumericComparison()) where.and(condition(c));
    var ranges =
        r.include().stream()
            .filter(c -> c.operator().isNumericComparison())
            .collect(java.util.stream.Collectors.groupingBy(Condition::attributeId));
    for (var conditions : ranges.values()) {
      var m = new QAlcoholAttributeMapping("candidateRangeMapping");
      var range =
          new BooleanBuilder(
              m.alcoholId
                  .eq(alcohol.id)
                  .and(m.attributeId.eq(conditions.getFirst().attributeId())));
      conditions.forEach(c -> range.and(numericValue(m, c)));
      where.and(JPAExpressions.selectOne().from(m).where(range).exists());
    }
    for (var c : r.exclude()) where.and(condition(c).not());
    if (r.abvFrom() != null || r.abvTo() != null) {
      var number = abvNumber();
      where.and(number.between(BigDecimal.ZERO, new BigDecimal("100")));
      if (r.abvFrom() != null) where.and(number.goe(r.abvFrom()));
      if (r.abvTo() != null) where.and(number.loe(r.abvTo()));
    }
    if (!r.tastingGroupCodes().isEmpty()) {
      if (query.tastingTagIds().isEmpty()) where.and(alcohol.id.lt(0L));
      else {
        var t = new QAlcoholsTastingTags("candidateFilterTag");
        where.and(
            JPAExpressions.selectOne()
                .from(t)
                .where(t.alcohol.id.eq(alcohol.id), t.tastingTag.id.in(query.tastingTagIds()))
                .exists());
      }
    }
    return where;
  }

  private NumberExpression<BigDecimal> abvNumber() {
    return Expressions.numberTemplate(
        BigDecimal.class,
        "case when function('regexp_like', trim({0}), {1}) = true then cast(replace(trim({0}), '%', '') as BigDecimal(24,6)) else null end",
        alcohol.abv,
        "^[0-9]+([.][0-9]+)?[[:space:]]*%?$");
  }

  private BooleanExpression condition(Condition c) {
    var m = new QAlcoholAttributeMapping("candidateConditionMapping");
    var base =
        JPAExpressions.selectOne()
            .from(m)
            .where(m.alcoholId.eq(alcohol.id), m.attributeId.eq(c.attributeId()));
    if (c.operator() == AttributeSearchOperator.EXISTS) return base.exists();
    if (c.operator() == AttributeSearchOperator.NOT_EXISTS) return base.notExists();
    if (c.operator() == AttributeSearchOperator.NE) {
      var different = new QAlcoholAttributeMapping("candidateEqualMapping");
      return base.exists()
          .and(
              JPAExpressions.selectOne()
                  .from(different)
                  .where(
                      different.alcoholId.eq(alcohol.id),
                      different.attributeId.eq(c.attributeId()),
                      different.attributeValue.eq(c.value()))
                  .notExists());
    }
    BooleanExpression value;
    if (c.operator().isNumericComparison()) {
      value = numericValue(m, c);
    } else
      value =
          c.operator() == AttributeSearchOperator.CONTAINS
              ? m.attributeValue.containsIgnoreCase(c.value())
              : m.attributeValue.eq(c.value());
    return base.where(value).exists();
  }

  private BooleanExpression numericValue(QAlcoholAttributeMapping mapping, Condition condition) {
    var n =
        Expressions.numberTemplate(
            BigDecimal.class, "cast({0} as BigDecimal(24,6))", mapping.attributeValue);
    var expected = new BigDecimal(condition.value());
    return switch (condition.operator()) {
      case GT -> n.gt(expected);
      case GTE -> n.goe(expected);
      case LT -> n.lt(expected);
      case LTE -> n.loe(expected);
      default -> throw new IllegalStateException("Unsupported numeric operator");
    };
  }
}
