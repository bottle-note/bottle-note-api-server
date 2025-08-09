package app.bottlenote.rating.dto.request;

import jakarta.validation.constraints.NotNull;

public record RatingRegisterRequest(
    @NotNull(message = "ALCOHOL_ID_REQUIRED") Long alcoholId,
    @NotNull(message = "RATING_REQUIRED") Double rating) {}
