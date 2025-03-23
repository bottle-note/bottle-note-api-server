package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.FollowStatus;
import lombok.Builder;

@Builder
public record RelationUserItem(
		Long userId,
		Long followUserId,
		String followUserNickname,
		String userProfileImage,
		FollowStatus status,
		Long reviewCount,
		Long ratingCount
) {
	public RelationUserItem(
			Long userId,
			Long followUserId,
			String followUserNickname,
			String userProfileImage,
			String status,
			Long reviewCount,
			Long ratingCount) {
		this(userId,
				followUserId,
				followUserNickname,
				userProfileImage,
				FollowStatus.parsing(status),
				reviewCount,
				ratingCount);
	}
}
