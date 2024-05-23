package app.bottlenote.follow.dto;


import app.bottlenote.follow.domain.constant.FollowStatus;
import jakarta.validation.constraints.NotNull;

public record FollowUpdateRequest(

	@NotNull(message = "팔로우 할 유저의 아이디가 없습니다.")
	Long followerUserId,

	@NotNull(message = "FOLLOWING, UNFOLLOW, BLOCK, HIDDEN 중 하나를 선택해주세요.")
	FollowStatus status

) {

}
