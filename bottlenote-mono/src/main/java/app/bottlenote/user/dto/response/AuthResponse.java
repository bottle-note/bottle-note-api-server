package app.bottlenote.user.dto.response;

public record AuthResponse(TokenItem token, Boolean isFirstLogin, String nickname) {}
