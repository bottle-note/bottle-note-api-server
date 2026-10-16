package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholAttribute.alcoholAttribute;
import static app.bottlenote.alcohols.domain.QAlcoholAttributeMapping.alcoholAttributeMapping;

import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.QAlcoholAttribute;
import app.bottlenote.alcohols.domain.QAlcoholAttributeMapping;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import app.bottlenote.global.pagination.OffsetPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
      request.value() == null ? null : alcoholAttribute.attributeValue.eq(request.value()),
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
