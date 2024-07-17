package app.bottlenote.like.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class LikeUserInfo {
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(name = "user_nick_name", nullable = false)
	private String userNickName;

	protected LikeUserInfo() {
	}

	protected LikeUserInfo(Long userId, String userNickName) {
		this.userId = userId;
		this.userNickName = userNickName;
	}

	public static LikeUserInfo create(Long userId, String userNickName) {
		return new LikeUserInfo(userId, userNickName);
	}
}
