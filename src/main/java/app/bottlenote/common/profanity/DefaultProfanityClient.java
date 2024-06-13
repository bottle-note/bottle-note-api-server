package app.bottlenote.common.profanity;


import org.springframework.stereotype.Component;

/**
 * 비속어 검출 클라이언트의 기본 구현체
 */
@Component
public class DefaultProfanityClient implements ProfanityClient {

	@Override
	public boolean containsProfanity(String text) {
		// TODO 비속어 검출 로직 구현 필요
		return false;
	}
}
