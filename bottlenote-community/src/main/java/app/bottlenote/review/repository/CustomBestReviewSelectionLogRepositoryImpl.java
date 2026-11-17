package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.review.domain.QBestReviewSelectionLog.bestReviewSelectionLog;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.review.constant.BestReviewChangeType;
import app.bottlenote.review.dto.request.AdminBestReviewSelectionLogSearchRequest;
import app.bottlenote.review.dto.response.AdminBestReviewSelectionLogResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** 관리자용 베스트 리뷰 선정 로그 조회. 최근 변동이 먼저 보이도록 기준일·ID 내림차순으로 고정한다. */
@RequiredArgsConstructor
public class CustomBestReviewSelectionLogRepositoryImpl
    implements CustomBestReviewSelectionLogRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<AdminBestReviewSelectionLogResponse> searchAdminLogs(
      AdminBestReviewSelectionLogSearchRequest request) {
    BooleanExpression[] filters = filters(request);

    List<AdminBestReviewSelectionLogResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminBestReviewSelectionLogResponse.class,
                    bestReviewSelectionLog.id,
                    bestReviewSelectionLog.selectionDate,
                    bestReviewSelectionLog.reviewId,
                    bestReviewSelectionLog.alcoholId,
                    alcohol.korName,
                    user.id,
                    user.nickName,
                    bestReviewSelectionLog.changeType,
                    bestReviewSelectionLog.ranking,
                    bestReviewSelectionLog.score,
                    bestReviewSelectionLog.likeCount,
                    bestReviewSelectionLog.dislikeCount,
                    bestReviewSelectionLog.replyCount,
                    bestReviewSelectionLog.imageCount,
                    bestReviewSelectionLog.contentLength,
                    bestReviewSelectionLog.alcoholReviewCount,
                    bestReviewSelectionLog.ruleCode,
                    bestReviewSelectionLog.reason,
                    bestReviewSelectionLog.createdAt))
            .from(bestReviewSelectionLog)
            .leftJoin(review)
            .on(review.id.eq(bestReviewSelectionLog.reviewId))
            .leftJoin(user)
            .on(user.id.eq(review.userId))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(bestReviewSelectionLog.alcoholId))
            .where(filters)
            .orderBy(bestReviewSelectionLog.selectionDate.desc(), bestReviewSelectionLog.id.desc())
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    Long total =
        queryFactory
            .select(bestReviewSelectionLog.id.count())
            .from(bestReviewSelectionLog)
            .where(filters)
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }

  private static BooleanExpression[] filters(AdminBestReviewSelectionLogSearchRequest request) {
    return new BooleanExpression[] {
      reviewIdEq(request.reviewId()),
      alcoholIdEq(request.alcoholId()),
      changeTypeEq(request.changeType()),
      selectedFromGoe(request.selectedFrom()),
      selectedToLoe(request.selectedTo())
    };
  }

  private static BooleanExpression reviewIdEq(Long reviewId) {
    return reviewId != null ? bestReviewSelectionLog.reviewId.eq(reviewId) : null;
  }

  private static BooleanExpression alcoholIdEq(Long alcoholId) {
    return alcoholId != null ? bestReviewSelectionLog.alcoholId.eq(alcoholId) : null;
  }

  private static BooleanExpression changeTypeEq(BestReviewChangeType changeType) {
    return changeType != null ? bestReviewSelectionLog.changeType.eq(changeType) : null;
  }

  private static BooleanExpression selectedFromGoe(LocalDate from) {
    return from != null ? bestReviewSelectionLog.selectionDate.goe(from) : null;
  }

  private static BooleanExpression selectedToLoe(LocalDate to) {
    return to != null ? bestReviewSelectionLog.selectionDate.loe(to) : null;
  }
}
