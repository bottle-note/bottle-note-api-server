package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholAttribute.alcoholAttribute;
import static app.bottlenote.alcohols.domain.QAlcoholAttributeMapping.alcoholAttributeMapping;

import app.bottlenote.alcohols.constant.AttributeMatch;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.QAlcoholAttribute;
import app.bottlenote.alcohols.domain.QAlcoholAttributeMapping;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeConditionRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCompoundSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.global.pagination.OffsetPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomAlcoholAttributeRepositoryImpl implements CustomAlcoholAttributeRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;

  @Override
  public OffsetPage<AlcoholAttribute> search(AdminAlcoholAttributeSearchRequest request) {
    BooleanExpression[] where = {
      request.type() == null ? null : alcoholAttribute.attributeType.eq(request.type()),
      keywordMatch(request.keyword()),
      request.alcoholIds().isEmpty()
          ? null
          : JPAExpressions.selectOne()
              .from(alcoholAttributeMapping)
              .where(
                  alcoholAttributeMapping.attributeId.eq(alcoholAttribute.id),
                  alcoholAttributeMapping.alcoholId.in(request.alcoholIds()))
              .exists()
    };
    List<AlcoholAttribute> content =
        queryFactory
            .selectFrom(alcoholAttribute)
            .where(where)
            .orderBy(alcoholAttribute.id.asc())
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();
    Long total =
        queryFactory
            .select(alcoholAttribute.count())
            .from(alcoholAttribute)
            .where(where)
            .fetchOne();
    return new OffsetPage<>(content, total == null ? 0 : total);
  }

  @Override
  public OffsetPage<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request) {
    BooleanExpression[] where = {
      supporter.isNotDeleted(),
      supporter.keywordMatch(request.keyword()),
      request.type() == null ? null : alcoholAttribute.attributeType.eq(request.type()),
      request.key() == null ? null : alcoholAttribute.attributeKey.eq(request.key()),
      valueMatch(request),
      notHavingKey(request.missingKey())
    };
    List<Long> content =
        queryFactory
            .select(alcohol.id)
            .from(alcoholAttributeMapping)
            .join(alcohol)
            .on(alcohol.id.eq(alcoholAttributeMapping.alcoholId))
            .join(alcoholAttribute)
            .on(alcoholAttribute.id.eq(alcoholAttributeMapping.attributeId))
            .where(where)
            .groupBy(alcohol.id, alcohol.korName)
            .orderBy(alcohol.korName.asc(), alcohol.id.asc())
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();
    Long total =
        queryFactory
            .select(alcohol.id.countDistinct())
            .from(alcoholAttributeMapping)
            .join(alcohol)
            .on(alcohol.id.eq(alcoholAttributeMapping.alcoholId))
            .join(alcoholAttribute)
            .on(alcoholAttribute.id.eq(alcoholAttributeMapping.attributeId))
            .where(where)
            .fetchOne();
    return new OffsetPage<>(content, total == null ? 0 : total);
  }

  @Override
  public OffsetPage<Long> findAlcoholIds(AdminAlcoholCompoundSearchRequest request) {
    BooleanExpression combined = null;
    for (int i = 0; i < request.conditions().size(); i++) {
      var predicate = conditionMatch(request.conditions().get(i), i);
      combined =
          combined == null
              ? predicate
              : request.match() == AttributeMatch.AND
                  ? combined.and(predicate)
                  : combined.or(predicate);
    }
    BooleanExpression linked = null;
    if (request.linkedOnly() || request.type() != null) {
      linked =
          JPAExpressions.selectOne()
              .from(alcoholAttributeMapping)
              .join(alcoholAttribute)
              .on(alcoholAttribute.id.eq(alcoholAttributeMapping.attributeId))
              .where(
                  alcoholAttributeMapping.alcoholId.eq(alcohol.id),
                  request.type() == null ? null : alcoholAttribute.attributeType.eq(request.type()))
              .exists();
    }
    BooleanExpression[] where = {
      supporter.isNotDeleted(), supporter.keywordMatch(request.keyword()), linked, combined
    };
    var content =
        queryFactory
            .select(alcohol.id)
            .from(alcohol)
            .where(where)
            .orderBy(alcohol.korName.asc(), alcohol.id.asc())
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();
    var total = queryFactory.select(alcohol.count()).from(alcohol).where(where).fetchOne();
    return new OffsetPage<>(content, total == null ? 0 : total);
  }

  private BooleanExpression conditionMatch(
      AdminAlcoholAttributeConditionRequest condition, int index) {
    var definition = new QAlcoholAttribute("conditionAttribute" + index);
    var mapping = new QAlcoholAttributeMapping("conditionMapping" + index);
    BooleanExpression value = null;
    if (condition.operator().requiresValue()) {
      var stored = mapping.attributeValue;
      if (condition.operator().isNumericComparison()) {
        var number =
            Expressions.numberTemplate(BigDecimal.class, "cast({0} as BigDecimal(24,6))", stored);
        var expected = new BigDecimal(condition.value());
        value =
            switch (condition.operator()) {
              case GT -> number.gt(expected);
              case GTE -> number.goe(expected);
              case LT -> number.lt(expected);
              case LTE -> number.loe(expected);
              default -> throw new IllegalStateException("Unsupported numeric operator");
            };
      } else {
        value =
            switch (condition.operator()) {
              case EQ -> stored.eq(condition.value());
              case NE -> stored.ne(condition.value());
              case CONTAINS -> stored.containsIgnoreCase(condition.value());
              default -> throw new IllegalStateException("Unsupported value operator");
            };
      }
    }
    var query =
        JPAExpressions.selectOne()
            .from(mapping)
            .join(definition)
            .on(definition.id.eq(mapping.attributeId))
            .where(
                mapping.alcoholId.eq(alcohol.id),
                definition.attributeKey.eq(condition.key()),
                value);
    return condition.operator() == AttributeSearchOperator.NOT_EXISTS
        ? query.notExists()
        : query.exists();
  }

  private BooleanExpression valueMatch(AdminAttributedAlcoholSearchRequest request) {
    if (request.value() == null) return null;
    if (request.operator() == AttributeOperator.EQ)
      return alcoholAttributeMapping.attributeValue.eq(request.value());
    return Expressions.numberTemplate(
            BigDecimal.class,
            "cast({0} as BigDecimal(24,6))",
            alcoholAttributeMapping.attributeValue)
        .goe(new BigDecimal(request.value()));
  }

  private BooleanExpression keywordMatch(String keyword) {
    if (keyword == null) return null;
    var name = alcoholAttribute.displayName.containsIgnoreCase(keyword);
    // ASCII 키 컬럼에는 한글을 비교하지 않아 MySQL collation 오류를 방지한다.
    return keyword.chars().allMatch(c -> c < 128)
        ? name.or(alcoholAttribute.attributeKey.containsIgnoreCase(keyword))
        : name;
  }

  private BooleanExpression notHavingKey(String key) {
    if (key == null) return null;
    QAlcoholAttribute other = new QAlcoholAttribute("missingAttribute");
    QAlcoholAttributeMapping mapping = new QAlcoholAttributeMapping("missingMapping");
    return JPAExpressions.selectOne()
        .from(mapping)
        .join(other)
        .on(other.id.eq(mapping.attributeId))
        .where(mapping.alcoholId.eq(alcohol.id), other.attributeKey.eq(key))
        .notExists();
  }
}
