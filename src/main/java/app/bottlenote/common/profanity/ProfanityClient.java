package app.bottlenote.common.profanity;

import org.springframework.http.ResponseEntity;

public interface ProfanityClient {

	/**
	 * 파라미터에 욕설이 포함되어 있는지 확인한다.
	 *
	 * @param text the text
	 * @return the boolean
	 */
	boolean containsProfanity(String text);

	ResponseEntity<?> newContainsProfanity(String word);
}
