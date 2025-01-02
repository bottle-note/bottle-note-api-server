package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowingSearchResponse(
	Long totalCount,
	List<FollowingDetail> followingList
) {
	public static FollowingSearchResponse of(Long totalCount, List<FollowingDetail> followingList) {
		return new FollowingSearchResponse(totalCount, followingList);
	}
}
