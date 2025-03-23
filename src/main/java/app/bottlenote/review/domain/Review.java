package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.common.image.ImageInfo;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.review.domain.constant.ReviewActiveStatus;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.constant.ReviewResultMessage;
import app.bottlenote.review.dto.request.LocationInfoRequest;
import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.review.facade.payload.ReviewModifyVO;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Builder
@ToString(includeFieldNames = false)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Comment("리뷰 테이블(리뷰, 평점, 이미지, 리뷰 댓글)")
@Entity(name = "review")
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("누가 쓴 리뷰인지")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("어떤 술에 대한 리뷰인지")
	@Column(name = "alcohol_id", nullable = false)
	private Long alcoholId;

	@Builder.Default
	@Comment("베스트리뷰 여부")
	@Column(name = "is_best", nullable = false)
	private Boolean isBest = false;

	@Builder.Default
	@Comment("리뷰 시점 평점")
	@Column(name = "review_rating")
	private Double reviewRating = 0.0;

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

	@Builder.Default
	@Comment("공개 상태")
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReviewDisplayStatus status = ReviewDisplayStatus.PUBLIC;

	@Comment("위치 정보")
	@Embedded
	private ReviewLocation reviewLocation;

	@Comment("썸네일 이미지")
	@Column(name = "image_url")
	private String imageUrl;

	@Builder.Default
	@Comment("조회수")
	@Column(name = "view_count", nullable = false)
	private Long viewCount = 0L;

	@Builder.Default
	@Comment("리뷰 활성 상태")
	@Column(name = "active_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReviewActiveStatus activeStatus = ReviewActiveStatus.ACTIVE;

	@Builder.Default
	@Comment("리뷰 이미지 (1급 컬렉션) ")
	@Embedded
	private ReviewImages reviewImages = ReviewImages.empty();

	@Builder.Default
	@Comment("리뷰 테이스팅 태그 (1급 컬렉션) ")
	@Embedded
	private ReviewTastingTags reviewTastingTags = ReviewTastingTags.empty();

	public void update(ReviewModifyVO reviewModifyVO) {
		this.status = reviewModifyVO.getReviewDisplayStatus();
		this.content = reviewModifyVO.getContent();
		this.sizeType = reviewModifyVO.getSizeType();
		this.price = reviewModifyVO.getPrice();
		LocationInfoRequest locationInfoRequest = reviewModifyVO.getLocationInfo();
		Objects.requireNonNullElse(this.reviewLocation, ReviewLocation.empty()).update(locationInfoRequest);
	}


	public void imageInitialization(List<ReviewImageInfoRequest> list) {
		list = Objects.requireNonNullElse(list, Collections.emptyList());
		List<ReviewImage> imageList = list.stream()
			.map(
				image ->
					ReviewImage.builder()
						.reviewImageInfo(
							ImageInfo.builder()
								.order(image.order())
								.imageUrl(image.viewUrl())
								.imagePath(ImageUtil.getImagePath(image.viewUrl()))
								.imageKey(ImageUtil.getImageKey(image.viewUrl()))
								.imageName(ImageUtil.getImageName(image.viewUrl()))
								.build()
						)
						.review(this)
						.build()
			).toList();

		if (list.size() > 1) {
			this.imageUrl = list.get(0).viewUrl();
		}
		updateImages(imageList);
		log.info("review id {} 의 썸네일 이미지 설정 url : {}", this.id, this.imageUrl);
	}

	public void updateImages(List<ReviewImage> reviewImages) {
		this.reviewImages.update(reviewImages);
	}


	public void updateDisplayStatus(ReviewDisplayStatus status) {
		this.status = status;
	}

	public void saveTastingTag(List<String> reviewTastingTags) {
		this.reviewTastingTags.saveReviewTastingTag(reviewTastingTags, this);
	}

	public void updateTastingTags(List<String> updateTastingTags) {
		this.reviewTastingTags.updateReviewTastingTags(updateTastingTags, this);
	}


	public ReviewResultMessage updateReviewActiveStatus(ReviewActiveStatus activeStatus) {
		this.activeStatus = activeStatus;
		return switch (activeStatus) {
			case ACTIVE -> ReviewResultMessage.ACTIVE_SUCCESS;
			case DELETED -> ReviewResultMessage.DELETE_SUCCESS;
			case DISABLED -> ReviewResultMessage.BLOCK_SUCCESS;
		};
	}

	public void blockReview() {
		this.activeStatus = ReviewActiveStatus.DISABLED;
	}
}
