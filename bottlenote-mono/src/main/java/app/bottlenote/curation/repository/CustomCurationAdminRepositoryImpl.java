package app.bottlenote.curation.repository;

import static app.bottlenote.curation.domain.QCuration.curation;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.dto.request.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class CustomCurationAdminRepositoryImpl implements CustomCurationAdminRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<Curation> searchForAdmin(
      String keyword,
      Long specId,
      Boolean isActive,
      Pageable pageable,
      CurationSortType sortType,
      SortOrder sortOrder) {
    BooleanExpression condition = keywordCondition(keyword).and(specCondition(specId)).and(activeCondition(isActive));
    List<Curation> content =
        queryFactory
            .selectFrom(curation)
            .where(condition)
            .orderBy(orderBy(sortType, sortOrder))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    Long total = queryFactory.select(curation.count()).from(curation).where(condition).fetchOne();
    return new PageImpl<>(content, pageable, total != null ? total : 0L);
  }

  private static OrderSpecifier<?>[] orderBy(CurationSortType sortType, SortOrder sortOrder) {
    if (sortType == CurationSortType.DISPLAY_ORDER) {
      return new OrderSpecifier<?>[] {sortOrder.resolve(curation.displayOrder), sortOrder.resolve(curation.id)};
    }
    return new OrderSpecifier<?>[] {
      new CaseBuilder().when(curation.exposureStartDate.isNull()).then(1).otherwise(0).asc(),
      sortOrder.resolve(curation.exposureStartDate),
      sortOrder.resolve(curation.id)
    };
  }

  private static BooleanExpression keywordCondition(String keyword) {
    return keyword == null || keyword.isBlank() ? curation.id.isNotNull() : curation.name.contains(keyword);
  }

  private static BooleanExpression specCondition(Long specId) {
    return specId == null ? curation.id.isNotNull() : curation.specId.eq(specId);
  }

  private static BooleanExpression activeCondition(Boolean isActive) {
    return isActive == null ? curation.id.isNotNull() : curation.isActive.eq(isActive);
  }
}
