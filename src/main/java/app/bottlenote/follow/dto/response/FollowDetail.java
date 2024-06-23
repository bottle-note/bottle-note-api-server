package app.bottlenote.follow.dto.response;

import app.bottlenote.follow.domain.constant.FollowStatus;



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
