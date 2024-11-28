package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;

public record FollowerDetail(
	Long userId,
	Long followUserId,
	String nickName,
	String userProfileImage,
	FollowStatus status,
	Long reviewCount,
	Long ratingCount
) {

	public static FollowerDetail of(Long userId, Long followUserId, String nickName, String userProfileImage, FollowStatus status, Long reviewCount, Long ratingCount) {
		return new FollowerDetail(userId, followUserId, nickName, userProfileImage, status, reviewCount, ratingCount);
	}
}
