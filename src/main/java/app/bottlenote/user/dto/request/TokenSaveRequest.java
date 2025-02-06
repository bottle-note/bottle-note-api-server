package app.bottlenote.user.dto.request;

import app.bottlenote.user.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TokenSaveRequest(
	@NotBlank(message = "DEVICE_TOKEN_REQUIRED")
	String deviceToken,
	@NotNull(message = "PLATFORM_REQUIRED")
	Platform platform
) {
}
