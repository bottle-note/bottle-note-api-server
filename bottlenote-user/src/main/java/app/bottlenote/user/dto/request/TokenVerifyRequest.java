package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenVerifyRequest(@NotBlank(message = "TOKEN_REQUIRED") String token) {}
