package app.bottlenote.review.repository.custom;

import app.bottlenote.review.domain.QReviewReply;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.SubReviewReplyInfo;
import app.bottlenote.user.domain.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.user.domain.QUser.user;

public class CustomReviewReplyRepositoryImpl implements CustomReviewReplyRepository {

	private static final Logger log = LogManager.getLogger(CustomReviewReplyRepositoryImpl.class);
	private final JPAQueryFactory queryFactory;

	public CustomReviewReplyRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public List<?> getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
		long start = System.nanoTime();

		List<ReviewReplyInfo> replyInfoList = queryFactory.select(
				Projections.constructor(
					ReviewReplyInfo.class,
					review.userId,
					user.imageUrl,
					user.nickName,
					reviewReply.id,
					reviewReply.content,
					reviewReply.createAt
				)
			)
			.from(review)
			.join(user).on(review.userId.eq(user.id))
			.leftJoin(reviewReply).on(review.id.eq(reviewReply.review.id))
			.where(
				review.id.eq(reviewId), // 리뷰 ID 일치
				reviewReply.rootReviewReply.isNull() // 최상위 댓글만 조회
			)
			.orderBy(reviewReply.createAt.desc())
			.offset(cursor) // 페이지 번호
			.limit(pageSize) // 페이지 사이즈
			.fetch();

		long end = System.nanoTime();
		log.debug("최상위 댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
		return replyInfoList;
	}

	@Override
	public List<?> getSubReviewReplies(Long reviewId, Long rootReplyId, Long cursor, Long pageSize) {
		long start = System.nanoTime();

		var parentReviewReply = new QReviewReply("parentReviewReply");
		var parentUser = new QUser("parentUser");

		List<SubReviewReplyInfo> subReplyInfoList = queryFactory.select(
				Projections.constructor(
					SubReviewReplyInfo.class,
					user.id,
					user.imageUrl,
					user.nickName,
					reviewReply.rootReviewReply.id,
					reviewReply.parentReviewReply.id,
					parentUser.nickName,
					reviewReply.id,
					reviewReply.content,
					reviewReply.createAt
				)
			).from(reviewReply)
			.join(user).on(reviewReply.userId.eq(user.id))
			.join(parentReviewReply).on(reviewReply.parentReviewReply.id.eq(parentReviewReply.id))
			.join(parentUser).on(reviewReply.parentReviewReply.userId.eq(parentUser.id))
			.where(
				reviewReply.review.id.eq(reviewId), // 리뷰 ID 일치
				reviewReply.rootReviewReply.id.eq(rootReplyId) // 부모 댓글 ID 일치
			)
			.orderBy(reviewReply.createAt.desc()) // 최신순
			.offset(cursor) // 페이지 번호
			.limit(pageSize) // 페이지 사이즈
			.fetch();

		long end = System.nanoTime();
		log.info("대댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
		return subReplyInfoList;
	}
}
