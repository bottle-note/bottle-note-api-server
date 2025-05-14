package app.bottlenote.review.facade.payload;

public record UserInfo(
	Long userId,
	String nickName,
	String userProfileImage
) {

	public static UserInfo of(Long userId, String nickName, String userProfileImage) {
		return new UserInfo(userId, nickName, userProfileImage);
	}
}
