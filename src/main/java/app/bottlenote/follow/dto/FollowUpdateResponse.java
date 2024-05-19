package app.bottlenote.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FollowUpdateResponse {
	private Long followUserId;
	private String message;

	@Builder
	public FollowUpdateResponse(Long followUserId, Message message) {
		this.followUserId = followUserId;
		this.message = message.getMessage();
	}

	@AllArgsConstructor
	@Getter
	public enum Message {
		FOLLOW_SUCCESS("성공적으로 팔로우 처리했습니다."),
		UNFOLLOW_SUCCESS("성공적으로 언팔로우 처리했습니다.");

		private final String message;
	}
}
