package app.bottlenote.follow.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FollowStatus {

	FOLLOWING,
	UNFOLLOW,
	BLOCK,
	HIDDEN;

	@JsonCreator
	public static FollowStatus parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return FollowStatus.valueOf(source.toUpperCase());
	}

}
