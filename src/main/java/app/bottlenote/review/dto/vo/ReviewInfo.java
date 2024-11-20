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
	Long reviewId,
	String reviewContent,
	BigDecimal price,
	SizeType sizeType,
	Long likeCount,
	Long replyCount,
	UserInfo userInfo,
	String reviewImageUrl,
	Double rating,
	Long viewCount,
	ReviewLocation locationInfo,
	ReviewDisplayStatus status,
	Boolean isMyReview,
	Boolean isLikedByMe,
	Boolean hasReplyByMe,
	Boolean isBestReview,

	@JsonSerialize(using = CustomSerializers.TastingTagListSerializer.class)
	@JsonDeserialize(using = CustomDeserializers.TastingTagListDeserializer.class)
	String tastingTagList,
	@JsonSerialize(using = CustomSerializers.LocalDateTimeSerializer.class)
	@JsonDeserialize(using = CustomDeserializers.LocalDateTimeDeserializer.class)
	LocalDateTime createAt
) {
}
