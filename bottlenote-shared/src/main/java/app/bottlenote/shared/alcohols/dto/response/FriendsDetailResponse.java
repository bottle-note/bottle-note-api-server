package app.bottlenote.shared.alcohols.dto.response;

import app.bottlenote.shared.users.payload.FriendItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class FriendsDetailResponse {
  private Long followerCount;
  private List<FriendItem> friends;
}
