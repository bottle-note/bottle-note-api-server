package app.bottlenote.review.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.domain.event.ReviewReplyRegistryEvent;
import app.bottlenote.user.domain.User;
import jakarta.persistence.CascadeType;
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
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
@Comment("리뷰 테이블(리뷰, 평점, 이미지, 리뷰 댓글)")
@Entity(name = "review")
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohol_id")
	private Alcohol alcohol;

	@Comment("내용")
	@Column(name = "content", nullable = false)
	private String content;

	@Comment("용량타입")
	@Column(name = "size_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private SizeType sizeType;

	@Comment("가격")
	@Column(name = "price", nullable = false)
	private BigDecimal price;

	@Comment("공개 상태")
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReviewStatus status = ReviewStatus.PUBLIC;

	@Comment("우편번호")
	@Column(name = "zip_code")
	private String zipCode;

	@Comment("주소")
	@Column(name = "address")
	private String address;

	@Comment("상세주소")
	@Column(name = "detail_address")
	private String detailAddress;

	@Comment("썸네일 이미지")
	@Column(name = "image_url")
	private String imageUrl;

	@Comment("조회수")
	@Column(name = "view_count", nullable = false)
	private Long viewCount = 0L;

	// 댓글 목록
	// review와 reviewReply는 1(review) : N(reviewReply) 관계이다. , orphanRemoval = true: review가 삭제되면 reviewReply도 삭제된다.
	// cascade = CascadeType.ALL: review가 저장될 때 reviewReply도 같이 저장된다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
	private List<ReviewReply> reviewReplies = new ArrayList<>();

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// review image와 review는 1(review) : N(reviewImage) 관계이다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
	private List<ReviewImage> reviewImages = new ArrayList<>();

	@Builder
	public Review(Long id, User user, Alcohol alcohol, String content, SizeType sizeType,
				  BigDecimal price, ReviewStatus status, String zipCode, String address, String detailAddress,
				  String imageUrl, Long viewCount, List<ReviewReply> reviewReplies,
				  List<ReviewImage> reviewImages) {
		this.id = id;
		this.user = user;
		this.alcohol = alcohol;
		this.content = content;
		this.sizeType = sizeType;
		this.price = price;
		this.status = status;
		this.zipCode = zipCode;
		this.address = address;
		this.detailAddress = detailAddress;
		this.imageUrl = imageUrl;
		this.viewCount = viewCount;
		this.reviewReplies = new ArrayList<>();
		this.reviewImages = new ArrayList<>();
	}

	public void addReply(ReviewReply reply) {
		this.reviewReplies.add(reply);

		this.registerEvent(
			new ReviewReplyRegistryEvent(
				this.alcohol.getId(),
				reply.getId(),
				reply.getUserId(),
				reply.getContent(),
				Optional.ofNullable(reply.getReview()).map(Review::getId).orElse(null)
			)
		);
	}
}
