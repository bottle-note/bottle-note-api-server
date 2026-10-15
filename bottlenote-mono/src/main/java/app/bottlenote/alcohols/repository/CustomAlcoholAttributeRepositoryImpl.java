package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholAttribute.alcoholAttribute;

import app.bottlenote.alcohols.domain.QAlcoholAttribute;
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class CustomAlcoholAttributeRepositoryImpl implements CustomAlcoholAttributeRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;

  public CustomAlcoholAttributeRepositoryImpl(
      JPAQueryFactory queryFactory, AlcoholQuerySupporter supporter) {
    this.queryFactory = queryFactory;
    this.supporter = supporter;
  }

  /** 조건에 맞는 속성 행이 있는 위스키를 한글 이름순으로 페이지로 돌려준다. 삭제된 위스키는 뺀다 */
  @Override
  public Page<Long> findAlcoholIds(AdminAttributedAlcoholSearchRequest request, Pageable pageable) {
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
            .select(alcoholAttribute.alcoholId)
            .from(alcoholAttribute)
            .join(alcohol)
            .on(alcohol.id.eq(alcoholAttribute.alcoholId))
            .where(where)
            .groupBy(alcoholAttribute.alcoholId, alcohol.korName)
            .orderBy(alcohol.korName.asc(), alcoholAttribute.alcoholId.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory
            .select(alcoholAttribute.alcoholId.countDistinct())
            .from(alcoholAttribute)
            .join(alcohol)
            .on(alcohol.id.eq(alcoholAttribute.alcoholId))
            .where(where)
            .fetchOne();

    return new PageImpl<>(content, pageable, total != null ? total : 0L);
  }

  private BooleanExpression notHavingKey(String missingKey) {
    if (missingKey == null) {
      return null;
    }
    QAlcoholAttribute other = new QAlcoholAttribute("missingAttribute");
    return JPAExpressions.selectOne()
        .from(other)
        .where(other.alcoholId.eq(alcoholAttribute.alcoholId), other.attributeKey.eq(missingKey))
        .notExists();
  }
}
