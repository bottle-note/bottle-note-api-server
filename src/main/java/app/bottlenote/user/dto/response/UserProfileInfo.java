package app.bottlenote.user.dto.response;

public record UserProfileInfo(
	Long id,
	String nickname,
	String imageUrl
) {
	public static UserProfileInfo create(Long id, String nickname, String imageUrl) {
		return new UserProfileInfo(id, nickname, imageUrl);
	}
}
