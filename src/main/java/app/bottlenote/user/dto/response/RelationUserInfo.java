package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;
import lombok.Builder;

@Builder
public record RelationUserInfo(
	Long userId,
	Long followUserId,
	String followUserNickname,
	String userProfileImage,
	FollowStatus status,
	Long reviewCount,
	Long ratingCount
) {
}
