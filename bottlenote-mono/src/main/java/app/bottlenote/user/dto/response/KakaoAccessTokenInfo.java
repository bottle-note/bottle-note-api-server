package app.bottlenote.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 액세스 토큰 정보. app_id로 토큰을 발급한 앱을 식별한다. */
public record KakaoAccessTokenInfo(
    Long id,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("app_id") Long appId) {}
