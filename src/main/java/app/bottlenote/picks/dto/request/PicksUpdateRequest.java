package app.bottlenote.picks.dto.request;

import app.bottlenote.picks.domain.PicksStatus;
import jakarta.validation.constraints.NotNull;

public record PicksUpdateRequest(

	@NotNull(message = "alcoholId는 필수입니다.")
	Long alcoholId,

	@NotNull(message = "isPicked는 필수입니다.")
	PicksStatus isPicked
) {
}
