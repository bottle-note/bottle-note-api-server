package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QCurationKeyword.curationKeyword;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordDto;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CustomCurationKeywordRepositoryImpl implements CustomCurationKeywordRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public CursorResponse<CurationKeywordDto> searchCurationKeywords(
		String keyword,
		Long alcoholId,
		Long cursor,
		Integer pageSize
	) {
		List<CurationKeywordDto> results = queryFactory
			.select(Projections.fields(
				CurationKeywordDto.class,
				curationKeyword.id.as("id"),
				curationKeyword.name.as("name"),
				curationKeyword.description.as("description"),
				curationKeyword.alcoholIds.size().as("alcoholCount"),
				curationKeyword.displayOrder.as("displayOrder")
			))
			.from(curationKeyword)
			.where(
				curationKeyword.isActive.isTrue(),
				keywordContains(keyword),
				alcoholIdIn(alcoholId),
				curationKeyword.id.gt(cursor)
			)
			.orderBy(
				curationKeyword.displayOrder.asc(),
				curationKeyword.id.desc()
			)
			.limit(pageSize + 1)
			.fetch();

		CursorPageable pageable = CursorPageable.of(results, cursor, pageSize);
		List<CurationKeywordDto> content = results.size() > pageSize
			? results.subList(0, pageSize)
			: results;

		return CursorResponse.of(content, pageable);
	}

	@Override
	public CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
		Long curationId,
		Long cursor,
		Integer pageSize
	) {
		CurationKeyword curation = queryFactory
			.selectFrom(curationKeyword)
			.where(curationKeyword.id.eq(curationId))
			.fetchOne();

		if (curation == null || curation.getAlcoholIds().isEmpty()) {
			return CursorResponse.of(List.of(), CursorPageable.builder()
				.currentCursor(cursor)
				.cursor(cursor)
				.pageSize((long) pageSize)
				.hasNext(false)
				.build());
		}

		List<Long> alcoholIdsList = curation.getAlcoholIds().stream().toList();

		List<AlcoholsSearchItem> results = queryFactory
			.select(Projections.fields(
				AlcoholsSearchItem.class,
				alcohol.id.as("alcoholId"),
				alcohol.korName.as("korName"),
				alcohol.engName.as("engName"),
				alcohol.korCategory.as("korCategoryName"),
				alcohol.engCategory.as("engCategoryName"),
				alcohol.imageUrl.as("imageUrl"),
				rating.ratingPoint.rating.avg()
					.multiply(2)
					.castToNum(Double.class)
					.round()
					.divide(2)
					.coalesce(0.0)
					.as("rating"),
				rating.id.countDistinct().as("ratingCount"),
				review.id.countDistinct().as("reviewCount"),
				picks.id.countDistinct().as("pickCount"),
				Expressions.asBoolean(false).as("isPicked")
			))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.id.alcoholId))
			.leftJoin(review).on(alcohol.id.eq(review.alcoholId))
			.leftJoin(picks).on(alcohol.id.eq(picks.alcoholId))
			.where(
				alcohol.id.in(alcoholIdsList),
				alcohol.id.gt(cursor)
			)
			.groupBy(
				alcohol.id,
				alcohol.korName,
				alcohol.engName,
				alcohol.korCategory,
				alcohol.engCategory,
				alcohol.imageUrl
			)
			.orderBy(createOrderByField(alcoholIdsList).asc())
			.limit(pageSize + 1)
			.fetch();

		CursorPageable pageable = CursorPageable.of(results, cursor, pageSize);
		List<AlcoholsSearchItem> content = results.size() > pageSize
			? results.subList(0, pageSize)
			: results;

		return CursorResponse.of(content, pageable);
	}

	private BooleanExpression keywordContains(String keyword) {
		return keyword != null && !keyword.isBlank()
			? curationKeyword.name.containsIgnoreCase(keyword)
			: null;
	}

	private BooleanExpression alcoholIdIn(Long alcoholId) {
		if (alcoholId == null) {
			return null;
		}

		return Expressions.numberTemplate(Long.class,
			"CASE WHEN {0} MEMBER OF {1} THEN 1 ELSE 0 END",
			alcoholId,
			curationKeyword.alcoholIds
		).eq(1L);
	}

	private NumberExpression<Integer> createOrderByField(List<Long> alcoholIds) {
		CaseBuilder caseBuilder = new CaseBuilder();
		for (int i = 0; i < alcoholIds.size(); i++) {
			caseBuilder.when(alcohol.id.eq(alcoholIds.get(i))).then(i);
		}
		return caseBuilder.otherwise(alcoholIds.size());
	}
}
