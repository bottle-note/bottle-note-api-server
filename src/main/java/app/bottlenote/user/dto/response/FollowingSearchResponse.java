package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowingSearchResponse(
	Long totalCount,
	List<RelationUserInfo> followingList
) {

	public static FollowingSearchResponse of(Long totalCount, List<RelationUserInfo> followingList) {
		return new FollowingSearchResponse(totalCount, followingList);
	}
}
