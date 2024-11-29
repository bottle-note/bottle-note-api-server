package app.bottlenote.alcohols.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class FriendsDetailInfo {

	private final Long followerCount;
	private final List<FriendInfo> friends;


	public record FriendInfo(
		String userImageUrl,
		Long userId,
		String nickName,
		Double rating) {
		public static FriendInfo of(String userImageUrl, Long userId, String nickName, Double rating) {
			return new FriendInfo(userImageUrl, userId, nickName, rating);
		}
	}
}
