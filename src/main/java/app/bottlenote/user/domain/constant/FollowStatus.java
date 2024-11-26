package app.bottlenote.user.domain.constant;

import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Stream;

public enum FollowStatus {

	FOLLOWING("팔로잉"),
	UNFOLLOW("언팔로우");

	private final String Description;

	FollowStatus(String description) {
		this.Description = description;
	}

	@JsonCreator
	public static FollowStatus parsing(String followStatus) {
		if (followStatus == null || followStatus.isEmpty()) {
			throw new FollowException(FollowExceptionCode.STATUS_NOT_FOUND);
		}

		return Stream.of(FollowStatus.values())
			.filter(status -> status.toString().equals(followStatus.toUpperCase()))
			.findFirst()
			.orElseThrow(() -> new FollowException(FollowExceptionCode.STATUS_NOT_FOUND));
	}

}
