package app.bottlenote.common.profanity;

import app.bottlenote.global.config.FeginConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
	name = "profanity",
	url = "${profanity.filter.url}",
	configuration = FeginConfig.class
)
public interface ProfanityFeginClient {

	@GetMapping("/filter/basic")
	ResponseEntity<ProfanityResult> callProfanityFilter(@RequestParam("word") String word);

}
