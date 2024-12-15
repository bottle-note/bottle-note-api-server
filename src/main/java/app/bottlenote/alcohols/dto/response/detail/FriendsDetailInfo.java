package app.bottlenote.alcohols.dto.response.detail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class FriendsDetailInfo {
	private final Long followerCount;
	private final List<FriendInfo> friends;
}
