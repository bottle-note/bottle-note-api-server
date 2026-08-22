package app.bottlenote.review.repository;

import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.core.types.ExpressionUtils.count;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.review.constant.ReviewReplyStatus;
import app.bottlenote.review.domain.QReviewReply;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse.Item;
import app.bottlenote.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomReviewReplyRepositoryImpl implements CustomReviewReplyRepository {

  private static final Logger log = LogManager.getLogger(CustomReviewReplyRepositoryImpl.class);
  private static final int DEFAULT_SIZE = 50;
  private static final int MAX_SIZE = 100;
  private final JPAQueryFactory queryFactory;
  private final HmacCursorCodec cursorCodec;

  public CustomReviewReplyRepositoryImpl(
      JPAQueryFactory queryFactory, HmacCursorCodec cursorCodec) {
    this.queryFactory = queryFactory;
    this.cursorCodec = cursorCodec;
  }

  @Override
  public KeysetPageResponse<RootReviewReplyResponse> getReviewRootReplies(
      Long reviewId, String cursor, Integer size) {
    long start = System.nanoTime();
    QReviewReply subReply = new QReviewReply("subReply");
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    String context = "review.reply.root:" + reviewId;

    List<RootReviewReplyResponse.Item> replyItemList =
        queryFactory
            .select(
                Projections.constructor(
                    RootReviewReplyResponse.Item.class,
                    reviewReply.userId,
                    user.imageUrl,
                    user.nickName,
                    reviewReply.id,
                    new CaseBuilder()
                        .when(reviewReply.status.eq(ReviewReplyStatus.DELETED))
                        .then(ReviewReplyStatus.DELETED.getMessage())
                        .otherwise(reviewReply.content),
                    queryFactory
                        .select(count(subReply.id))
                        .from(subReply)
                        .where(subReply.rootReviewReply.id.eq(reviewReply.id)),
                    reviewReply.status,
                    reviewReply.createAt))
            .from(reviewReply)
            .join(user)
            .on(reviewReply.userId.eq(user.id))
            .where(
                reviewReply.reviewId.eq(reviewId),
                reviewReply.rootReviewReply.isNull(),
                replySeek(page.cursor(), context, false))
            .groupBy(reviewReply.id)
            .orderBy(reviewReply.createAt.desc(), reviewReply.id.desc())
            .limit(page.size() + 1L)
            .fetch();

    long end = System.nanoTime();
    log.debug("최상위 댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
    KeysetPagination.PageSlice<RootReviewReplyResponse.Item> slice =
        KeysetPagination.fromOverflow(
            replyItemList,
            page.size(),
            item ->
                cursorCodec.encode(
                    context, TimeIdCursor.keys(item.createAt(), item.reviewReplyId())));
    return KeysetPageResponse.of(RootReviewReplyResponse.of(slice.items()), slice.pagination());
  }

  @Override
  public KeysetPageResponse<SubReviewReplyResponse> getSubReviewReplies(
      Long reviewId, Long rootReplyId, String cursor, Integer size) {
    long start = System.nanoTime();
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    String context = "review.reply.sub:" + reviewId + ":" + rootReplyId;

    var parentReviewReply = new QReviewReply("parentReviewReply");
    var parentUser = new QUser("parentUser");

    List<Item> subReplyItemList =
        queryFactory
            .select(
                Projections.constructor(
                    Item.class,
                    user.id,
                    user.imageUrl,
                    user.nickName,
                    reviewReply.rootReviewReply.id,
                    reviewReply.parentReviewReply.id,
                    parentUser.nickName,
                    reviewReply.id,
                    new CaseBuilder()
                        .when(reviewReply.status.eq(ReviewReplyStatus.DELETED))
                        .then(ReviewReplyStatus.DELETED.getMessage())
                        .otherwise(reviewReply.content),
                    reviewReply.status,
                    reviewReply.createAt))
            .from(reviewReply)
            .join(user)
            .on(reviewReply.userId.eq(user.id))
            .join(parentReviewReply)
            .on(reviewReply.parentReviewReply.id.eq(parentReviewReply.id))
            .join(parentUser)
            .on(reviewReply.parentReviewReply.userId.eq(parentUser.id))
            .where(
                reviewReply.reviewId.eq(reviewId),
                reviewReply.rootReviewReply.id.eq(rootReplyId),
                replySeek(page.cursor(), context, true))
            .orderBy(reviewReply.createAt.asc(), reviewReply.id.asc())
            .limit(page.size() + 1L)
            .fetch();

    long end = System.nanoTime();
    log.info("대댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
    KeysetPagination.PageSlice<Item> slice =
        KeysetPagination.fromOverflow(
            subReplyItemList,
            page.size(),
            item ->
                cursorCodec.encode(
                    context, TimeIdCursor.keys(item.createAt(), item.reviewReplyId())));
    return KeysetPageResponse.of(SubReviewReplyResponse.of(slice.items()), slice.pagination());
  }

  private BooleanExpression replySeek(String cursor, String context, boolean ascending) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    var claims = cursorCodec.verify(cursor, context);
    LocalDateTime lastCreateAt = TimeIdCursor.time(claims);
    Long lastId = TimeIdCursor.id(claims);
    if (ascending) {
      return reviewReply
          .createAt
          .gt(lastCreateAt)
          .or(reviewReply.createAt.eq(lastCreateAt).and(reviewReply.id.gt(lastId)));
    }
    return reviewReply
        .createAt
        .lt(lastCreateAt)
        .or(reviewReply.createAt.eq(lastCreateAt).and(reviewReply.id.lt(lastId)));
  }
}
