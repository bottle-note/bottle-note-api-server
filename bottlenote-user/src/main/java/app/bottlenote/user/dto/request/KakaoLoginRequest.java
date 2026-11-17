package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
    @NotBlank(message = "KAKAO_ACCESS_TOKEN_REQUIRED") String accessToken) {}
