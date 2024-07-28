package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.review.domain.constant.ReviewReplyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Comment("리뷰 댓글 테이블")
@Entity(name = "review_reply")
public class ReviewReply extends BaseEntity {
	@Getter
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Getter
	@Comment("댓글 대상")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Getter
	@Comment("댓글 작성 유저")
	@Column(name = "user_id")
	private Long userId;

	@Comment("댓글 내용")
	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReviewReplyStatus status = ReviewReplyStatus.NORMAL;

	@Comment("최상위 댓글 대상")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "root_reply_id")
	private ReviewReply rootReviewReply;

	@Getter
	@Comment("상위 댓글 대상")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_reply_id")
	private ReviewReply parentReviewReply;

	@Getter
	@Comment("대댓글 목록")
	@OneToMany(mappedBy = "parentReviewReply", fetch = FetchType.LAZY)
	private List<ReviewReply> replies = new ArrayList<>();

	protected ReviewReply() {
	}

	@Builder
	public ReviewReply(Long id, Review review, Long userId, String content, ReviewReplyStatus status, ReviewReply rootReviewReply, ReviewReply parentReviewReply, List<ReviewReply> replies) {
		this.id = id;
		this.review = review;
		this.userId = userId;
		this.content = content;
		this.status = Objects.isNull(status) ? ReviewReplyStatus.NORMAL : status;
		this.rootReviewReply = rootReviewReply;
		this.parentReviewReply = parentReviewReply;
		this.replies = replies;
	}

	/**
	 * 최상위 댓글 대상을 반환합니다.
	 * <p>
	 * 만약 최상위 댓글 대상이 없다면 자기 자신이 최상위 댓글 대상입니다.
	 *
	 * @return the root review reply
	 */
	public ReviewReply getRootReviewReply() {
		return rootReviewReply != null ? rootReviewReply : this;
	}

	/**
	 * 삭제된 메시지의 경우 상태값 메시지를 반환합니다.
	 *
	 * @return the content
	 */
	public String getContent() {
		if (!this.status.equals(ReviewReplyStatus.NORMAL)) {
			return this.status.getMessage();
		}
		return content;
	}

	/**
	 * string format => ReviewReply(id = 1, userId = 1, content = "content")
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" +
			"id = " + id + ", " +
			"userId = " + userId + ", " +
			"content = " + content + ")";
	}
}
