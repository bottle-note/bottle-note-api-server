package app.bottlenote.follow.domain.constant;

import app.bottlenote.follow.exception.FollowException;
import app.bottlenote.follow.exception.FollowExceptionCode;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum FollowStatus {

	FOLLOWING("팔로잉"),
	UNFOLLOW("언팔로우"),
	BLOCK("차단"),
	HIDDEN("숨김");

	private final String Description;

	FollowStatus(String description) {
		this.Description = description;
	}

	@JsonCreator
	public static FollowStatus parsing(String followStatus) {
		if (followStatus == null || followStatus.isEmpty()) {
			throw new FollowException(FollowExceptionCode.STATUS_NOT_FOUND);
		}
		return FollowStatus.valueOf(followStatus.toUpperCase());
	}

}
