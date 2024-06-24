package app.bottlenote.review.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.review.domain.constant.ReviewActiveStatus;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

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

	@Comment("리뷰 활성 상태")
	@Column(name = "active_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReviewActiveStatus activeStatus = ReviewActiveStatus.ACTIVE;

	// 댓글 목록
	// review와 reviewReply는 1(review) : N(reviewReply) 관계이다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReviewReply> reviewReplies = new ArrayList<>();

	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Likes> reviewLikes = new ArrayList<>();

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// review image와 review는 1(review) : N(reviewImage) 관계이다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReviewImage> reviewImages = new ArrayList<>();

	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<ReviewTastingTag> reviewTastingTags = new HashSet<>();

	@Builder
	public Review(Long id, User user, Alcohol alcohol, String content, SizeType sizeType, BigDecimal price, ReviewStatus status, String zipCode, String address, String detailAddress, String imageUrl, Long viewCount,
		ReviewActiveStatus activeStatus) {
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
		this.activeStatus = ReviewActiveStatus.ACTIVE;
		this.reviewReplies = new ArrayList<>();
		this.reviewImages = new ArrayList<>();
		this.reviewTastingTags = new HashSet<>();
		this.reviewLikes = new ArrayList<>();
	}

	public void modifyReview(ReviewModifyVO reviewModifyVO) {
		this.status = reviewModifyVO.getReviewStatus();
		this.content = reviewModifyVO.getContent();
		this.sizeType = reviewModifyVO.getSizeType();
		this.price = reviewModifyVO.getPrice();
		this.zipCode = reviewModifyVO.getZipCode();
		this.address = reviewModifyVO.getAddress();
		this.detailAddress = reviewModifyVO.getDetailAddress();
	}

	public void updateTastingTags(Set<ReviewTastingTag> updateTastingTags) {
		this.reviewTastingTags.clear();
		this.reviewTastingTags.addAll(updateTastingTags);
	}

	public void updateImages(List<ReviewImage> reviewImages) {
		this.reviewImages.clear();
		this.reviewImages.addAll(reviewImages);
	}

	public void saveTastingTag(Set<ReviewTastingTag> reviewTastingTags) {
		this.reviewTastingTags.addAll(reviewTastingTags);
	}

	public void saveImages(List<ReviewImage> reviewImageList) {
		this.reviewImages.addAll(reviewImageList);
	}

	public void updateReviewActiveStatus(ReviewActiveStatus activeStatus) {
		this.activeStatus = activeStatus;
	}
}
