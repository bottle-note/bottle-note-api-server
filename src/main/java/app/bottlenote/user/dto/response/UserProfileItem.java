package app.bottlenote.user.dto.response;

public record UserProfileItem(
	Long id,
	String nickname,
	String imageUrl
) {
	public static UserProfileItem create(Long id, String nickname, String imageUrl) {
		return new UserProfileItem(id, nickname, imageUrl);
	}
}
