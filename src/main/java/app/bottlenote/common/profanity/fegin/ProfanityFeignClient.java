package app.bottlenote.common.profanity.fegin;

import app.bottlenote.common.profanity.dto.request.ProfanityRequest;
import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import app.bottlenote.global.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "profanity", url = "${profanity.filter.url}", configuration = FeignConfig.class)
public interface ProfanityFeignClient {
  @PostMapping(value = "/filter", headers = "x-api-key=${profanity.filter.key}")
  ResponseEntity<ProfanityResponse> requestVerificationProfanity(
      @RequestBody ProfanityRequest text);
}
