package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowerSearchResponse(
	Long totalCount,
	List<RelationUserInfo> followerList
) {

	public static FollowerSearchResponse of(Long totalCount, List<RelationUserInfo> followerList) {
		return new FollowerSearchResponse(totalCount, followerList);
	}
}
