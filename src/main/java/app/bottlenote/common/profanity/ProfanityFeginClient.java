package app.bottlenote.common.profanity;

import app.bottlenote.global.config.FeginConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = "profanity",
	url = "${profanity.filter.url}",
	configuration = FeginConfig.class
)
public interface ProfanityFeginClient {

	@PostMapping("/filter/basic")
	ResponseEntity<ProfanityResult> callProfanityFilter(@RequestBody String word);
}
