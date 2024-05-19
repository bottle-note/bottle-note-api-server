package app.bottlenote.follow.dto;


import jakarta.validation.constraints.NotNull;

public record FollowUpdateRequest(

	@NotNull(message = "팔로우 할 유저의 아이디가 없습니다.")
	Long followerUserId,

	@NotNull(message = "isFollow 값이 없습니다.")
	boolean isFollow

) {

}
