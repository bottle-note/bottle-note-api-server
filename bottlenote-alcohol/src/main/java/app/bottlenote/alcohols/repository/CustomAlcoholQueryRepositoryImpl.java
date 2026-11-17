package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.constant.AlcoholType.WHISKY;
import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;

  public CustomAlcoholQueryRepositoryImpl(
      JPAQueryFactory queryFactory, AlcoholQuerySupporter supporter) {
    this.queryFactory = queryFactory;
    this.supporter = supporter;
  }

  /** 모든 카테고리(한글, 영문, 그룹) 조회 — 카테고리 레퍼런스 응답용 */
  @Override
  public List<CategoryItem> findAllCategoryItems() {
    return queryFactory
        .select(
            Projections.constructor(
                CategoryItem.class,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.categoryGroup))
        .from(alcohol)
        .where(alcohol.type.eq(WHISKY), supporter.isNotDeleted())
        .groupBy(alcohol.korCategory, alcohol.engCategory, alcohol.categoryGroup)
        .orderBy(alcohol.korCategory.asc())
        .fetch();
  }

  @Override
  public List<AlcoholLookupItem> findAllLookupItems() {
    return queryFactory
        .select(
            Projections.constructor(
                AlcoholLookupItem.class,
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.categoryGroup,
                region.id,
                region.korName,
                region.engName,
                distillery.id,
                distillery.korName,
                distillery.engName,
                alcohol.imageUrl))
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.type.eq(WHISKY), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findAllMatchTargets() {
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByDistilleryIdIn(List<Long> distilleryIds) {
    if (distilleryIds == null || distilleryIds.isEmpty()) {
      return List.of();
    }
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.distillery.id.in(distilleryIds), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByIdIn(List<Long> alcoholIds) {
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.id.in(alcoholIds), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  private Expression<AlcoholMatchTargetItem> matchTargetProjection() {
    return Projections.constructor(
        AlcoholMatchTargetItem.class,
        alcohol.id,
        alcohol.korName,
        alcohol.engName,
        alcohol.abv,
        alcohol.age,
        alcohol.korCategory,
        alcohol.engCategory,
        region.id,
        region.korName,
        region.engName,
        distillery.id,
        distillery.korName,
        distillery.engName,
        alcohol.imageUrl,
        alcohol.volume);
  }

  /** queryDSL 리뷰 상세 조회 시 포함 될 술의 정보를 조회합니다. 찜 여부는 AlcoholFacade 구현이 채운다. */
  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {

    return Optional.ofNullable(
        queryFactory
            .select(
                Projections.constructor(
                    AlcoholSummaryItem.class,
                    alcohol.id.as("alcoholId"),
                    alcohol.korName.as("korName"),
                    alcohol.engName.as("engName"),
                    alcohol.korCategory.as("korCategoryName"),
                    alcohol.engCategory.as("engCategoryName"),
                    alcohol.imageUrl.as("imageUrl"),
                    Expressions.asBoolean(false)))
            .from(alcohol)
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(alcohol.id.eq(alcoholId), supporter.isNotDeleted())
            .groupBy(
                alcohol.id,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.imageUrl,
                alcohol.korName,
                alcohol.engName,
                region.korName,
                region.engName,
                alcohol.cask,
                alcohol.abv,
                distillery.korName,
                distillery.engName)
            .fetchOne());
  }

  /** Admin용 알코올 검색 (Offset 페이징) */
  @Override
  public Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    List<AdminAlcoholItem> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminAlcoholItem.class,
                    alcohol.id,
                    alcohol.korName,
                    alcohol.engName,
                    alcohol.korCategory,
                    alcohol.engCategory,
                    alcohol.imageUrl,
                    alcohol.createAt,
                    alcohol.lastModifyAt,
                    alcohol.deletedAt))
            .from(alcohol)
            .where(
                supporter.keywordMatch(request.keyword()),
                supporter.eqCategory(request.category()),
                supporter.eqRegion(request.regionId()),
                Boolean.TRUE.equals(request.includeDeleted()) ? null : supporter.isNotDeleted())
            .orderBy(supporter.sortByAdmin(request.sortType(), request.sortOrder()))
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    Long total =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .where(
                supporter.keywordMatch(request.keyword()),
                supporter.eqCategory(request.category()),
                supporter.eqRegion(request.regionId()),
                Boolean.TRUE.equals(request.includeDeleted()) ? null : supporter.isNotDeleted())
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }
}
