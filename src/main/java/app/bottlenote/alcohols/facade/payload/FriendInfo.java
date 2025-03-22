package app.bottlenote.alcohols.facade.payload;

public record FriendInfo(
	String userImageUrl,
	Long userId,
	String nickName,
	Double rating) {

	public static FriendInfo of(String userImageUrl, Long userId, String nickName, Double rating) {
		return new FriendInfo(userImageUrl, userId, nickName, rating);
	}
}
