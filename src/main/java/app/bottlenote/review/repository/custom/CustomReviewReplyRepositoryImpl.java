package app.bottlenote.review.repository.custom;

import app.bottlenote.review.domain.QReviewReply;
import app.bottlenote.review.domain.constant.ReviewReplyStatus;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse.Item;
import app.bottlenote.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.core.types.ExpressionUtils.count;

public class CustomReviewReplyRepositoryImpl implements CustomReviewReplyRepository {

	private static final Logger log = LogManager.getLogger(CustomReviewReplyRepositoryImpl.class);
	private final JPAQueryFactory queryFactory;

	public CustomReviewReplyRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public RootReviewReplyResponse getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
		long start = System.nanoTime();
		QReviewReply subReply = new QReviewReply("subReply");

		List<RootReviewReplyResponse.Item> replyItemList = queryFactory.select(
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
					reviewReply.createAt
				)
			)
			.from(reviewReply)
			.join(user).on(reviewReply.userId.eq(user.id))
			.where(
				reviewReply.reviewId.eq(reviewId), // 리뷰 ID 일치
				reviewReply.rootReviewReply.isNull() // 최상위 댓글만 조회
			)
			.groupBy(reviewReply.id)
			.orderBy(reviewReply.createAt.desc())
			.offset(cursor)
			.limit(pageSize)
			.fetch();


		Long totalCount = queryFactory.select(count(reviewReply.id))
			.from(reviewReply)
			.where(
				reviewReply.reviewId.eq(reviewId),
				reviewReply.rootReviewReply.isNull()
			)
			.fetchOne();

		long end = System.nanoTime();
		log.debug("최상위 댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");

		return RootReviewReplyResponse.of(totalCount, replyItemList);
	}

	@Override
	public SubReviewReplyResponse getSubReviewReplies(Long reviewId, Long rootReplyId, Long cursor, Long pageSize) {
		long start = System.nanoTime();

		var parentReviewReply = new QReviewReply("parentReviewReply");
		var parentUser = new QUser("parentUser");

		List<Item> subReplyItemList = queryFactory.select(
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
					reviewReply.createAt
				)
			).from(reviewReply)
			.join(user).on(reviewReply.userId.eq(user.id))
			.join(parentReviewReply).on(reviewReply.parentReviewReply.id.eq(parentReviewReply.id))
			.join(parentUser).on(reviewReply.parentReviewReply.userId.eq(parentUser.id))
			.where(
				reviewReply.reviewId.eq(reviewId), // 리뷰 ID 일치
				reviewReply.rootReviewReply.id.eq(rootReplyId) // 부모 댓글 ID 일치
			)
			.orderBy(reviewReply.createAt.asc()) // 과거 댓글부터 조회
			.offset(cursor) // 페이지 번호
			.limit(pageSize) // 페이지 사이즈
			.fetch();

		Long totalCount = queryFactory.select(count(reviewReply.id))
			.from(reviewReply)
			.where(
				reviewReply.reviewId.eq(reviewId),
				reviewReply.rootReviewReply.id.eq(rootReplyId)
			)
			.fetchOne();

		long end = System.nanoTime();
		log.info("대댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
		return SubReviewReplyResponse.of(totalCount, subReplyItemList);
	}

}
