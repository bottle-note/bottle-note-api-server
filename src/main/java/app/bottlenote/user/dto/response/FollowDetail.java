package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;


public record FollowDetail(
	Long userId,
	Long followUserId,
	String nickName,
	String userProfileImage,
	FollowStatus status,
	Long reviewCount,
	Long ratingCount
) {

	public static FollowDetail of(Long userId, Long followUserId, String nickName, String userProfileImage, FollowStatus status, Long reviewCount, Long ratingCount) {
		return new FollowDetail(userId, followUserId, nickName, userProfileImage, status, reviewCount, ratingCount);
	}

}
