package app.bottlenote.user.dto.response;

import lombok.Builder;

@Builder
public record BasicAccountResponse(
    String message, String email, String nickname, String accessToken, String refreshToken) {}
