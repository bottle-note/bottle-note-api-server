package app.bottlenote.review.fixture;

import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.profanity.FakeProfanityFeginClient;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.request.ProfanityRequest;
import app.bottlenote.common.profanity.response.ProfanityResponse;
import org.springframework.http.ResponseEntity;

import static app.bottlenote.common.exception.CommonExceptionCode.CONTAINS_PROFANITY;

public class FakeProfanityClient implements ProfanityClient {

	FakeProfanityFeginClient fakeProfanityFeginClient = new FakeProfanityFeginClient();

	@Override
	public ProfanityResponse requestVerificationProfanity(String text) {
		var filter = ProfanityRequest.createFilter(text);
		ResponseEntity<ProfanityResponse> profanityResponseResponseEntity = fakeProfanityFeginClient.requestVerificationProfanity(filter);
		return profanityResponseResponseEntity.getBody();
	}

	@Override
	public String getFilteredText(String text) {
		ProfanityResponse response = requestVerificationProfanity(text);
		return response.filtered();
	}

	@Override
	public void validateProfanity(String text) {
		ProfanityResponse response = requestVerificationProfanity(text);
		if (!response.detected().isEmpty()) {
			throw new CommonException(CONTAINS_PROFANITY);
		}
	}
}
