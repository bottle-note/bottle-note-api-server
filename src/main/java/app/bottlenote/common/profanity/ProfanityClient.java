package app.bottlenote.common.profanity;

import app.bottlenote.common.profanity.response.ProfanityResponse;

/**
 * The interface Profanity client.
 */
public interface ProfanityClient {

	/**
	 * 파라미터에 욕설이 포함되어 있는지 확인한다.
	 *
	 * @param text the text
	 * @return the boolean
	 */
	ProfanityResponse requestVerificationProfanity(String text);

	/**
	 * 필터링 된 텍스트를 반환한다.
	 *
	 * @param text 필터링 대상 텍스트
	 * @return 필터링 된 텍스트
	 */
	String getFilteredText(String text);

	/**
	 * 욕설이 포함되어 있는지 확인한다.
	 * 만약 욕설이 포함되어 있다면, 예외를 발생시킨다.
	 *
	 * @param text the text
	 */
	void validateProfanity(String text);
}
