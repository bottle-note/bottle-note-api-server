package app.bottlenote.user.repository;

import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.repository.PicksQuerySupporter;
import app.bottlenote.rating.repository.RatingQuerySupporter;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.repository.ReviewQuerySupporter;
import app.bottlenote.user.constant.AdminUserSortType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.AdminUserSearchRequest;
import app.bottlenote.user.dto.response.AdminUserListResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class QueryDslAdminUserQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final ReviewQuerySupporter reviewQuerySupporter;
  private final RatingQuerySupporter ratingQuerySupporter;
  private final PicksQuerySupporter pickQuerySupporter;

  public QueryDslAdminUserQueryRepository(
      JPAQueryFactory queryFactory,
      ReviewQuerySupporter reviewQuerySupporter,
      RatingQuerySupporter ratingQuerySupporter,
      PicksQuerySupporter pickQuerySupporter) {
    this.queryFactory = queryFactory;
    this.reviewQuerySupporter = reviewQuerySupporter;
    this.ratingQuerySupporter = ratingQuerySupporter;
    this.pickQuerySupporter = pickQuerySupporter;
  }

  public Page<AdminUserListResponse> searchAdminUsers(AdminUserSearchRequest request) {
    Expression<Long> reviewCountExpr = reviewQuerySupporter.reviewCountSubQuery(user.id);
    Expression<Long> ratingCountExpr = ratingQuerySupporter.ratingCountSubQuery(user.id);
    Expression<Long> picksCountExpr = pickQuerySupporter.picksCountSubQuery(user.id);

    List<AdminUserRow> rows =
        queryFactory
            .select(
                Projections.constructor(
                    AdminUserRow.class,
                    user.id,
                    user.email,
                    user.nickName,
                    user.imageUrl,
                    user.role,
                    user.status,
                    reviewCountExpr,
                    ratingCountExpr,
                    picksCountExpr,
                    user.createAt,
                    user.lastLoginAt))
            .from(user)
            .where(adminUserKeyword(request.keyword()), adminUserStatus(request.status()))
            .orderBy(adminUserSortOrder(request.sortType(), request.sortOrder()))
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    // socialType 배치 로딩
    List<Long> userIds = rows.stream().map(AdminUserRow::userId).toList();
    Map<Long, List<SocialType>> socialTypeMap =
        userIds.isEmpty()
            ? Map.of()
            : queryFactory.selectFrom(user).where(user.id.in(userIds)).fetch().stream()
                .collect(Collectors.toMap(User::getId, User::getSocialType));

    List<AdminUserListResponse> content =
        rows.stream()
            .map(
                row ->
                    new AdminUserListResponse(
                        row.userId(),
                        row.email(),
                        row.nickName(),
                        row.imageUrl(),
                        row.role(),
                        row.status(),
                        socialTypeMap.getOrDefault(row.userId(), List.of()),
                        row.reviewCount(),
                        row.ratingCount(),
                        row.picksCount(),
                        row.createAt(),
                        row.lastLoginAt()))
            .toList();

    Long total =
        queryFactory
            .select(user.id.count())
            .from(user)
            .where(adminUserKeyword(request.keyword()), adminUserStatus(request.status()))
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }

  private BooleanExpression adminUserKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return user.nickName.containsIgnoreCase(keyword).or(user.email.containsIgnoreCase(keyword));
  }

  private BooleanExpression adminUserStatus(UserStatus status) {
    if (status == null) {
      return null;
    }
    return user.status.eq(status);
  }

  private OrderSpecifier<?> adminUserSortOrder(AdminUserSortType sortType, SortOrder sortOrder) {
    Order order = sortOrder == SortOrder.ASC ? Order.ASC : Order.DESC;
    return switch (sortType) {
      case CREATED_AT -> new OrderSpecifier<>(order, user.createAt);
      case NICK_NAME -> new OrderSpecifier<>(order, user.nickName);
      case EMAIL -> new OrderSpecifier<>(order, user.email);
      case REVIEW_COUNT ->
          new OrderSpecifier<>(
              order,
              select(review.count())
                  .from(review)
                  .where(
                      review
                          .userId
                          .eq(user.id)
                          .and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE))));
      case RATING_COUNT ->
          new OrderSpecifier<>(
              order,
              select(rating.count())
                  .from(rating)
                  .where(rating.id.userId.eq(user.id).and(rating.ratingPoint.rating.gt(0.0))));
    };
  }

  /** QueryDSL 프로젝션용 중간 레코드 (socialType은 JSON 컬럼이라 별도 처리) */
  public record AdminUserRow(
      Long userId,
      String email,
      String nickName,
      String imageUrl,
      UserType role,
      UserStatus status,
      Long reviewCount,
      Long ratingCount,
      Long picksCount,
      LocalDateTime createAt,
      LocalDateTime lastLoginAt) {}
}
