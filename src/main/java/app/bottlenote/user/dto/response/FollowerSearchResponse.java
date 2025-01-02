package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowerSearchResponse(
	Long totalCount,
	List<FollowerDetail> followerList
) {
	public static FollowerSearchResponse of(Long totalCount, List<FollowerDetail> followerList) {
		return new FollowerSearchResponse(totalCount, followerList);
	}
}
