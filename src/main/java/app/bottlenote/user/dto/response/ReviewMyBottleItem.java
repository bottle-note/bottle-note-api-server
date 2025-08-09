package app.bottlenote.user.dto.response;

import app.bottlenote.user.dto.response.MyBottleResponse.BaseMyBottleInfo;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;

@Builder
public record ReviewMyBottleItem(
    BaseMyBottleInfo baseMyBottleInfo,
    Long reviewId,
    boolean isMyReview,
    LocalDateTime reviewModifyAt,
    String reviewContent,
    Set<String> reviewTastingTags,
    boolean isBestReview) {}
