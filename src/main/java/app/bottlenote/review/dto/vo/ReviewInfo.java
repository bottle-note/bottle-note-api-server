package app.bottlenote.review.dto.vo;

import app.bottlenote.global.data.serializers.CustomDeserializers;
import app.bottlenote.global.data.serializers.CustomSerializers;
import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ReviewInfo(
	// 기본 리뷰 정보
	Long reviewId,
	String reviewContent,
	String reviewImageUrl,
	LocalDateTime createAt,
	Long totalImageCount,

	// 사용자 정보
	UserInfo userInfo,
	Boolean isMyReview,

	// 리뷰 상태 및 속성
	ReviewDisplayStatus status,
	Boolean isBestReview,
	ReviewLocation locationInfo,
	SizeType sizeType,

	// 가격 및 평점 정보
	BigDecimal price,
	Double rating,

	// 좋아요 및 댓글 정보
	Long likeCount,
	Long replyCount,
	Boolean isLikedByMe,
	Boolean hasReplyByMe,

	// 기타 정보
	Long viewCount,
	@JsonSerialize(using = CustomSerializers.TastingTagListSerializer.class)
	@JsonDeserialize(using = CustomDeserializers.TastingTagListDeserializer.class)
	String tastingTagList
) {
}
