package app.bottlenote.banner.repository;

import static app.bottlenote.banner.domain.QBanner.banner;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.dto.request.AdminBannerSearchRequest;
import app.bottlenote.banner.dto.response.AdminBannerListResponse;
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
public class CustomBannerRepositoryImpl implements CustomBannerRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<AdminBannerListResponse> searchForAdmin(
      AdminBannerSearchRequest request, Pageable pageable) {

    List<AdminBannerListResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminBannerListResponse.class,
                    banner.id,
                    banner.name,
                    banner.bannerType,
                    banner.sortOrder,
                    banner.isActive,
                    banner.startDate,
                    banner.endDate,
                    banner.createAt))
            .from(banner)
            .where(
                keywordContains(request.keyword()),
                isActiveEq(request.isActive()),
                bannerTypeEq(request.bannerType()))
            .orderBy(banner.sortOrder.asc(), banner.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory
            .select(banner.count())
            .from(banner)
            .where(
                keywordContains(request.keyword()),
                isActiveEq(request.isActive()),
                bannerTypeEq(request.bannerType()))
            .fetchOne();

    return new PageImpl<>(content, pageable, total != null ? total : 0L);
  }

  private BooleanExpression keywordContains(String keyword) {
    return keyword != null && !keyword.isBlank() ? banner.name.containsIgnoreCase(keyword) : null;
  }

  private BooleanExpression isActiveEq(Boolean isActive) {
    return isActive != null ? banner.isActive.eq(isActive) : null;
  }

  private BooleanExpression bannerTypeEq(BannerType bannerType) {
    return bannerType != null ? banner.bannerType.eq(bannerType) : null;
  }
}
