package app.bottlenote.user.dto.request;

import app.bottlenote.user.constant.FollowStatus;
import jakarta.validation.constraints.NotNull;

public record FollowUpdateRequest(
    @NotNull(message = "IS_NO_USER_ID_TO_FOLLOW") Long followUserId,
    @NotNull(message = "SELECT_FOLLOWING_OR_UNFOLLOW") FollowStatus status) {}
