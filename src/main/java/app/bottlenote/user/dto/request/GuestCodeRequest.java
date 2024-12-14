package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GuestCodeRequest(
	@NotBlank(message = "REQUIRED_GUEST_CODE")
	String code
) {
}
