package app.bottlenote.review.fixture;

import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.profanity.FakeProfanityFeignClient;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.request.ProfanityRequest;
import app.bottlenote.common.profanity.response.ProfanityResponse;
import org.springframework.http.ResponseEntity;

import static app.bottlenote.common.exception.CommonExceptionCode.CONTAINS_PROFANITY;

/**
 * ProfanityClient 의 fake 구현체입니다.
 */
public class FakeProfanityClient implements ProfanityClient {

	FakeProfanityFeignClient fakeProfanityFeignClient = new FakeProfanityFeignClient();

	@Override
	public ProfanityResponse requestVerificationProfanity(String text) {
		var filter = ProfanityRequest.createFilter(text);
		ResponseEntity<ProfanityResponse> profanityResponseResponseEntity = fakeProfanityFeignClient.requestVerificationProfanity(filter);
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
