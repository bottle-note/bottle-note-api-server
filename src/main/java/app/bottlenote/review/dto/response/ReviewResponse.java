package app.bottlenote.review.dto.response;

import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReviewResponse {

	private Long reviewId;
	private BigDecimal price;
	private SizeType sizeType;
	private Long likeCount;
	private Long replyCount;

	private Long userId;
	private String userNickname;
	private String userProfileImage;

	private Boolean myCommentYn;
	private String status;
	private Boolean myLikeYn;
	private Boolean myReplyYn;
	private String thumbnailImage;

	@Builder
	public ReviewResponse(Long reviewId, BigDecimal price, SizeType sizeType, Long likeCount,
		Long replyCount, Long userId, String userNickname, String userProfileImage,
		Boolean myCommentYn,
		String status, Boolean myLikeYn, Boolean myReplyYn, String thumbnailImage) {
		this.reviewId = reviewId;
		this.price = price;
		this.sizeType = sizeType;
		this.likeCount = likeCount;
		this.replyCount = replyCount;
		this.userId = userId;
		this.userNickname = userNickname;
		this.userProfileImage = userProfileImage;
		this.myCommentYn = myCommentYn;
		this.status = status;
		this.myLikeYn = myLikeYn;
		this.myReplyYn = myReplyYn;
		this.thumbnailImage = thumbnailImage;
	}

	public ReviewResponse() {
	}
}
