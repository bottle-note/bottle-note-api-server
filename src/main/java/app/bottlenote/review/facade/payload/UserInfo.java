package app.bottlenote.review.facade.payload;

public record UserInfo(
	Long userId,
	String nickName,
	String userProfileImage
) {
}
