package app.bottlenote.review.fixture;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.response.ProfanityResponse;

public class FakeProfanityClient implements ProfanityClient {
	@Override
	public ProfanityResponse requestVerificationProfanity(String text) {
		return null;
	}

	@Override
	public String getFilteredText(String text) {
		return "";
	}

	@Override
	public void validateProfanity(String text) {

	}
}
