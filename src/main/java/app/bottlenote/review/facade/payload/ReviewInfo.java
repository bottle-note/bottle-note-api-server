package app.bottlenote.review.facade.payload;

import app.bottlenote.common.block.annotation.BlockWord;
import app.bottlenote.global.data.serializers.CustomDeserializers.TagListDeserializer;
import app.bottlenote.global.data.serializers.CustomSerializers.TagListSerializer;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.ReviewLocation;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ReviewInfo(
    // 기본 리뷰 정보
    Long reviewId,
    @BlockWord(userIdPath = "userInfo.userId") String reviewContent,
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
    @JsonSerialize(using = TagListSerializer.class)
        @JsonDeserialize(using = TagListDeserializer.class)
        String tastingTagList) {}
