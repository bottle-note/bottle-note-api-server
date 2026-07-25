package app.bottlenote.user.client;

import app.bottlenote.global.config.FeignConfig;
import app.bottlenote.user.dto.response.KakaoAccessTokenInfo;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "kakao", url = "https://kapi.kakao.com", configuration = FeignConfig.class)
public interface KakaoFeignClient {

  @GetMapping("/v2/user/me")
  ResponseEntity<KakaoUserResponse> getUserInfo(
      @RequestHeader("Authorization") String authorization);

  /** 토큰을 발급한 앱(app_id)과 만료 시간을 조회한다. */
  @GetMapping("/v1/user/access_token_info")
  ResponseEntity<KakaoAccessTokenInfo> getAccessTokenInfo(
      @RequestHeader("Authorization") String authorization);
}
