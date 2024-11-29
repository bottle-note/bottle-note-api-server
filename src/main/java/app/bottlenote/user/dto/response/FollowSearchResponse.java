package app.bottlenote.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class FollowSearchResponse {

	private final Long totalCount;
	private final List<FollowingDetail> followingList;
	private final List<FollowerDetail> followerList;
}
