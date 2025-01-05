package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;


public record RelationUserInfo(
	Long userId,
	Long followUserId,
	String nickName,
	String userProfileImage,
	FollowStatus status,
	Long reviewCount,
	Long ratingCount
) {

	public static RelationUserInfo of(Long userId, Long followUserId, String nickName, String userProfileImage, FollowStatus status, Long reviewCount, Long ratingCount) {
		return new RelationUserInfo(userId, followUserId, nickName, userProfileImage, status, reviewCount, ratingCount);
	}
}
