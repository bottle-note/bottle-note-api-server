package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class ReviewDetail {

	private Long reviewId;
	private String reviewContent;
	private BigDecimal price;
	private SizeType sizeType;
	private Long likeCount;
	private Long replyCount;
	private String reviewImageUrl;
	private LocalDateTime createAt;

	private Long userId;
	private String nickName;
	private String userProfileImage;
	private Double rating;

	private String zipCode;
	private String address;
	private String detailAddress;

	private ReviewDisplayStatus status;

	private Boolean isMyReview;
	private Boolean isLikedByMe;
	private Boolean hasReplyByMe;

	private List<String> reviewTastingTag;

	@Builder
	public ReviewDetail(Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType, Long likeCount, Long replyCount, String reviewImageUrl, LocalDateTime createAt, Long userId, String nickName, String userProfileImage,
		Double rating, String zipCode, String address, String detailAddress, ReviewDisplayStatus status, Boolean isMyReview, Boolean isLikedByMe, Boolean hasReplyByMe, List<String> reviewTastingTag) {
		this.reviewId = reviewId;
		this.reviewContent = reviewContent;
		this.price = price;
		this.sizeType = sizeType;
		this.likeCount = likeCount;
		this.replyCount = replyCount;
		this.reviewImageUrl = reviewImageUrl;
		this.createAt = createAt;
		this.userId = userId;
		this.nickName = nickName;
		this.userProfileImage = userProfileImage;
		this.rating = rating;
		this.zipCode = zipCode;
		this.address = address;
		this.detailAddress = detailAddress;
		this.status = status;
		this.isMyReview = isMyReview;
		this.isLikedByMe = isLikedByMe;
		this.hasReplyByMe = hasReplyByMe;
		this.reviewTastingTag = reviewTastingTag;
	}

	public void updateTastingTagList(List<String> reviewTastingTag) {
		this.reviewTastingTag = reviewTastingTag;
	}
}
