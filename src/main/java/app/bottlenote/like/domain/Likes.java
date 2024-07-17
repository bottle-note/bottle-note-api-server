package app.bottlenote.like.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.review.domain.Review;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity(name = "likes")
@NoArgsConstructor(access = PROTECTED)
public class Likes extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Embedded
	private LikeUserInfo userInfo;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private LikeStatus status = LikeStatus.LIKE;


	@Builder
	public Likes(Long id, Review review, LikeUserInfo userInfo, LikeStatus status) {
		this.id = id;
		this.review = review;
		this.userInfo = userInfo;
		this.status = status;
	}
}
