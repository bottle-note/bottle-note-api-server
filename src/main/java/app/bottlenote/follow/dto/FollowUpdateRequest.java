package app.bottlenote.follow.dto;


import app.bottlenote.follow.domain.constant.FollowStatus;
import jakarta.validation.constraints.NotNull;

import static app.bottlenote.global.security.SecurityUtil.getCurrentUserId;

public record FollowUpdateRequest(

	@NotNull(message = "팔로우 할 유저의 아이디가 없습니다.")
	Long followerUserId,

	@NotNull(message = "isFollow 값이 없습니다.")
	FollowStatus isFollow // FOLLOWING 또는 UNFOLLOW

) {

}
