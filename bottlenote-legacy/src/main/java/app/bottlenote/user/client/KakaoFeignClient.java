package app.bottlenote.user.client;

import app.bottlenote.global.config.FeignConfig;
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
}
