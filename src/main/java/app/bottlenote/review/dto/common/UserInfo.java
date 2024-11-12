package app.bottlenote.review.dto.common;

public record UserInfo(
	Long userId,
	String nickName,
	String userProfileImage
) {
}
