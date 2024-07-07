package app.bottlenote.review.repository.custom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;

public class CustomReviewReplyRepositoryImpl implements CustomReviewReplyRepository {

	private static final Logger log = LogManager.getLogger(CustomReviewReplyRepositoryImpl.class);
	private final JPAQueryFactory queryFactory;

	public CustomReviewReplyRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public List<?> getReviewRootReplies(Long reviewId, Long cursor, Long pageSize) {
		long start = System.nanoTime();

		List<Tuple> fetch = queryFactory.select()
			.from(review)
			.leftJoin(reviewReply).on(review.id.eq(reviewReply.review.id))
			.where(
				review.id.eq(reviewId), // 리뷰 ID 일치
				reviewReply.rootReviewReply.isNotNull() // 최상위 댓글만 조회
			)
			.orderBy(reviewReply.createAt.desc())
			.offset(cursor) // 페이지 번호
			.limit(pageSize) // 페이지 사이즈
			.fetch();

		long end = System.nanoTime();
		log.debug("최상위 댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
		return fetch;
	}

	@Override
	public List<?> getReviewChildReplies(Long reviewId, Long parentReplyId, Long cursor, Long pageSize) {
		long start = System.nanoTime();


		long end = System.nanoTime();
		log.debug("대 댓글 목록 조회 시간 : {}", (end - start) / 1_000_000 + "ms");
		return List.of();
	}
}
