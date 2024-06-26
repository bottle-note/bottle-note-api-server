package app.bottlenote.follow.dto.response;

import app.bottlenote.follow.domain.constant.FollowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FollowUpdateResponse {
	private final Long followUserId;
	private final String nickName;
	private final String imageUrl;
	private final String message;

	@Builder
	public FollowUpdateResponse(Long followUserId, String nickName, String imageUrl, FollowStatus status) {
		this.followUserId = followUserId;
        this.nickName = nickName;
        this.imageUrl = imageUrl;
        this.message = status == FollowStatus.FOLLOWING ?
			Message.FOLLOW_SUCCESS.getMessage() : Message.UNFOLLOW_SUCCESS.getMessage();
	}

	@AllArgsConstructor
	@Getter
	public enum Message {
		FOLLOW_SUCCESS("성공적으로 팔로우 처리했습니다."),
		UNFOLLOW_SUCCESS("성공적으로 팔로우 해제 처리했습니다.");

		private final String message;
	}
}
