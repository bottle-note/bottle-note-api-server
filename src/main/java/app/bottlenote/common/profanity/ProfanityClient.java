package app.bottlenote.common.profanity;

public interface ProfanityClient {

	/**
	 * 파라미터에 욕설이 포함되어 있는지 확인한다.
	 *
	 * @param text the text
	 * @return the boolean
	 */
	ProfanityResult containsProfanity(String text);

	/**
	 * 욕설이 포함되어 있는지 확인한다.
	 * 만약 욕설이 포함되어 있다면, 예외를 발생시킨다.
	 *
	 * @param text the text
	 */
	void validateProfanity(String text);
}
