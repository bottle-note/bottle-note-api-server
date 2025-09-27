package app.bottlenote.picks.dto.request;

import app.bottlenote.shared.picks.constant.PicksStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PicksUpdateRequest(
    @Min(value = 1, message = "ALCOHOL_ID_MINIMUM") @NotNull(message = "ALCOHOL_ID_REQUIRED")
        Long alcoholId,
    @NotNull(message = "IS_PICKED_REQUIRED") PicksStatus isPicked) {}
