package app.bottlenote.common.profanity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profanity")
public class DefaultProfanityClientController {
	private final ProfanityClient profanityClient;

	public DefaultProfanityClientController(ProfanityClient profanityClient) {
		this.profanityClient = profanityClient;
	}

	@GetMapping
	public ResponseEntity<?> containsProfanity(
		@RequestParam(value = "word") String word
	) {
		return profanityClient.newContainsProfanity(word);
	}
}
