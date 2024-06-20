package app.bottlenote.common.profanity;


import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.exception.CommonExceptionCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * 비속어 검출 클라이언트의 기본 구현체
 */
@Component
public class DefaultProfanityClient implements ProfanityClient {

	private static final Logger log = LogManager.getLogger(DefaultProfanityClient.class);
	private final ProfanityFeginClient profanityFeginClient;

	public DefaultProfanityClient(ProfanityFeginClient profanityFeginClient) {
		this.profanityFeginClient = profanityFeginClient;
	}

	@Override
	public ProfanityResult containsProfanity(String text) {
		log.info("검증 요청 대상: {}", text);

		long start = System.currentTimeMillis();  // 요청 시작 시간 측정

		ResponseEntity<ProfanityResult> response = profanityFeginClient.callProfanityFilter(text);

		long end = System.currentTimeMillis();  // 요청 종료 시간 측정

		log.info("검증 완료 : {}", response);
		log.info("응답 시간 : {} ms", end - start);  // 응답 시간 로그 출력

		return response.getBody();
	}

	@Override
	public void validateProfanity(String text) {
		ProfanityResult result = containsProfanity(text);

		if (result.isProfanity()) {
			throw new CommonException(CommonExceptionCode.CONTAINS_PROFANITY);
		}
	}
}
