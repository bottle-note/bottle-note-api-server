package app.bottlenote.user.facade.payload;

public record FriendItem(
		String userImageUrl,
		Long userId,
		String nickName,
		Double rating) {

	public static FriendItem of(
			String userImageUrl,
			Long userId,
			String nickName,
			Double rating
	) {
		return new FriendItem(userImageUrl, userId, nickName, rating);
	}
}
