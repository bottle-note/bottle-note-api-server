package app.bottlenote.user.dto.request;

import lombok.Builder;

public record FollowPageableRequest(
	Long cursor,
	Long pageSize
) {
	@Builder
	public FollowPageableRequest {
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 50L;
	}
}
