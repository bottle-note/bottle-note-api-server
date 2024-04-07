package app.bottlenote.review.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.Users;
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
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Comment("리뷰 테이블(리뷰, 평점, 이미지, 리뷰 댓글)")
@Entity(name = "review")
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohols_id")
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
	private Long price;

	// 추후 주소 값으로 @Embedded를 사용하여 객체로 관리할 수 있음 (Address)
	@Comment("우편번호")
	@Column(name = "zip_code")
	private Long zipCode;

	@Comment("주소")
	@Column(name = "address")
	private String address;

	@Comment("상세주소")
	@Column(name = "detail_address")
	private String detailAddress;

	@Comment("썸네일 이미지")
	@Column(name = "image_url")
	private String iamgeUrl;

	//viewCnt X 축약어를 자제하세요. 좋은 코드는 읽기 쉬워야 합니다.
	// 기본 값은 0으로 설정한다.
	@Comment("조회수")
	@Column(name = "view_count", nullable = false, columnDefinition = "BIGINT default 0")
	private Long viewCount;

	// 댓글 목록
	// review와 reviewReply는 1(review) : N(reviewReply) 관계이다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
	private List<ReviewReply> reviewReplies = new ArrayList<>();

	// mappedBy: 연관관계의 주인이 아님을 의미한다.
	// review image와 review는 1(review) : N(reviewImage) 관계이다.
	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
	private List<ReviewImage> reviewImages = new ArrayList<>();


}

