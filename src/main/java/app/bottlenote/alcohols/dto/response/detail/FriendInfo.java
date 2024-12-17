package app.bottlenote.alcohols.dto.response.detail;

public record FriendInfo(
	String userImageUrl,
	Long userId,
	String nickName,
	Double rating) {

	public static FriendInfo of(String userImageUrl, Long userId, String nickName, Double rating) {
		return new FriendInfo(userImageUrl, userId, nickName, rating);
	}
}
