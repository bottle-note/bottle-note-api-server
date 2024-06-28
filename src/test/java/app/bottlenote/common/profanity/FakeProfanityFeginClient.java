package app.bottlenote.common.profanity;

import app.bottlenote.common.profanity.fegin.ProfanityFeginClient;
import app.bottlenote.common.profanity.request.ProfanityRequest;
import app.bottlenote.common.profanity.response.Detected;
import app.bottlenote.common.profanity.response.ProfanityResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class FakeProfanityFeginClient implements ProfanityFeginClient {


	private static final List<String> words = List.of("비속어", "욕설", "개자식");
	private static final String trackingId = "019054c5-dfb1-777d-b567-95688c87f135";
	private static final Logger log = LogManager.getLogger(FakeProfanityFeginClient.class);

	private ProfanityResponse createProfanityResponse(final String text) {
		log.info("[Fake] createProfanityResponse  욕설 검사 결과 : {}", text);
		String filtered = text;

		for (String word : words) {
			filtered = filtered.replaceAll(word, "*".repeat(word.length()));
		}
		List<Detected> list = words.stream()
			.filter(text::contains)
			.map(Detected::create)
			.toList();

		var status = new ProfanityResponse.Status(2000, "message", "description", "detailDescription");

		return ProfanityResponse.builder()
			.trackingId(trackingId)
			.status(status)
			.detected(list)
			.filtered(filtered)
			.elapsed("0.00005961 s / 0.05961 ms / 59.605 µs")
			.build();
	}

	@Override
	public ResponseEntity<ProfanityResponse> requestVerificationProfanity(ProfanityRequest request) {
		log.info("[Fake] requestVerificationProfanity 욕설 검사 요청 : {}", request);
		return ResponseEntity.ok(createProfanityResponse(request.text()));
	}
}
