package app.bottlenote.user.dto.dsl;

public record FollowPageableCriteria(
	Long cursor,
	Long pageSize
) {
	public static FollowPageableCriteria of(Long cursor, Long pageSize) {
		return new FollowPageableCriteria(
			cursor,
			pageSize
		);
	}
}
