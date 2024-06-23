package app.bottlenote.follow.dto.dsl;

public record FollowPageableCriteria (
	Long cursor,
	Long pageSize,
	Long userId
) {
	public static FollowPageableCriteria of(Long cursor, Long pageSize, Long userId){
		return new FollowPageableCriteria(
			cursor,
			pageSize,
			userId

		);
	}
}
