package app.external.push.dto.request;

import app.external.push.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TokenSaveRequest(
	@NotBlank(message = "DEVICE_TOKEN_REQUIRED")
	String deviceToken,
	@NotNull(message = "PLATFORM_REQUIRED")
	Platform platform
) {
}
