package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QCurationKeyword.curationKeyword;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class CustomCurationKeywordRepositoryImpl implements CustomCurationKeywordRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public java.util.Optional<java.util.Set<Long>> findAlcoholIdsByKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return java.util.Optional.empty();
    }

    CurationKeyword result =
        queryFactory
            .selectFrom(curationKeyword)
            .where(
                curationKeyword.isActive.isTrue(), curationKeyword.name.containsIgnoreCase(keyword))
            .fetchFirst();

    return java.util.Optional.ofNullable(result).map(CurationKeyword::getAlcoholIds);
  }

  private BooleanExpression keywordContains(String keyword) {
    return keyword != null && !keyword.isBlank()
        ? curationKeyword.name.containsIgnoreCase(keyword)
        : null;
  }

  @Override
  public Page<AdminCurationListResponse> searchForAdmin(
      AdminCurationSearchRequest request, Pageable pageable) {

    List<AdminCurationListResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminCurationListResponse.class,
                    curationKeyword.id,
                    curationKeyword.name,
                    curationKeyword.alcoholIds.size(),
                    curationKeyword.displayOrder,
                    curationKeyword.isActive,
                    curationKeyword.createAt))
            .from(curationKeyword)
            .where(keywordContains(request.keyword()), isActiveEq(request.isActive()))
            .orderBy(curationKeyword.displayOrder.asc(), curationKeyword.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory
            .select(curationKeyword.count())
            .from(curationKeyword)
            .where(keywordContains(request.keyword()), isActiveEq(request.isActive()))
            .fetchOne();

    return new PageImpl<>(content, pageable, total != null ? total : 0L);
  }

  private BooleanExpression isActiveEq(Boolean isActive) {
    return isActive != null ? curationKeyword.isActive.eq(isActive) : null;
  }
}
