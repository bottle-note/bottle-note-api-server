package app.bottlenote.like.dto.request;

import app.bottlenote.like.constant.LikeStatus;
import jakarta.validation.constraints.NotNull;

public record LikesUpdateRequest(
    @NotNull(message = "REVIEW_ID_REQUIRED") Long reviewId,
    @NotNull(message = "REVIEW_DISPLAY_STATUS_NOT_EMPTY") LikeStatus status) {}
