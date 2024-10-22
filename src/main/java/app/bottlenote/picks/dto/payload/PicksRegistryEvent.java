package app.bottlenote.picks.dto.payload;

import app.bottlenote.picks.domain.PicksStatus;

public record PicksRegistryEvent(
	Long alcoholId,
	Long userId,
	PicksStatus picksStatus
) {

	public static PicksRegistryEvent of(Long alcoholId, Long userId, PicksStatus picksStatus) {
		return new PicksRegistryEvent(alcoholId, userId, picksStatus);
	}
}
