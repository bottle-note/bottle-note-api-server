package app.bottlenote.support.help.dto.request;

import lombok.Builder;

public record HelpPageableRequest(
	Long cursor,
	Long pageSize
) {

	@Builder
	public HelpPageableRequest {
		cursor = cursor != null ? cursor : 0L;
		pageSize = pageSize != null ? pageSize : 10L;
	}
}
