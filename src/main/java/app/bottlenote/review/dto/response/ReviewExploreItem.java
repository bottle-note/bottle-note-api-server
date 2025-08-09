package app.bottlenote.review.dto.response;

import app.bottlenote.review.facade.payload.UserInfo;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewExploreItem(

    // 사용자 정보 속성
    UserInfo userInfo,
    Boolean isMyReview,

    // 술 정보 속성
    Long alcoholId,
    String alcoholName,

    // 리뷰 정보 속성
    Long reviewId,
    String reviewContent,
    Double reviewRating,
    List<String> reviewTags,
    LocalDateTime createAt,
    LocalDateTime modifiedAt,
    Long totalImageCount,
    List<String> reviewImages,

    // 좋아요 및 댓글 정보
    // 리뷰 상태 및 속성
    Boolean isBestReview,
    Long likeCount,
    Boolean isLikedByMe,
    Long replyCount,
    Boolean hasReplyByMe) {}
