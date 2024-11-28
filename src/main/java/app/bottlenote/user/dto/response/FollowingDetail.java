package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;


public record FollowingDetail(
	Long userId,
	Long followUserId,
	String nickName,
	String userProfileImage,
	FollowStatus status,
	Long reviewCount,
	Long ratingCount
) {

	public static FollowingDetail of(Long userId, Long followUserId, String nickName, String userProfileImage, FollowStatus status, Long reviewCount, Long ratingCount) {
		return new FollowingDetail(userId, followUserId, nickName, userProfileImage, status, reviewCount, ratingCount);
	}
}
