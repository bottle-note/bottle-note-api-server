package app.bottlenote.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class FollowUpdateResponse {

	private String message;
	private Long followUserId;

	@Builder
	public FollowUpdateResponse(String message, Long followUserId) {
		this.message = message;
		this.followUserId = followUserId;
	}

	@AllArgsConstructor
	@Getter
	public enum Message {
		FOLLOW_SUCCESS("성공적으로 팔로우 처리했습니다."),
		UNFOLLOW_SUCCESS("성공적으로 언팔로우 처리했습니다.");
		private final String message;
	}

}
