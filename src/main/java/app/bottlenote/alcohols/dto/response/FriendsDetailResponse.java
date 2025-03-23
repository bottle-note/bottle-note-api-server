package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.facade.payload.FriendInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class FriendsDetailResponse {
	private Long followerCount;
	private List<FriendInfo> friends;
}
