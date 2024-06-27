package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Comment("리뷰 댓글 테이블")
@Entity(name = "review_reply")
public class ReviewReply extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("댓글 대상")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Comment("댓글 작성 유저")
	@Column(name = "user_id")
	private Long userId;

	@Comment("댓글 내용")
	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	@Comment("대댓글 댓글 대상")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_reply_id")
	private ReviewReply parentReviewReply;

	@Comment("대댓글 목록")
	@OneToMany(mappedBy = "parentReviewReply", fetch = FetchType.LAZY)
	private List<ReviewReply> replies = new ArrayList<>();
}
