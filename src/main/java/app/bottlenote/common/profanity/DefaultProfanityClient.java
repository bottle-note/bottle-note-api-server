package app.bottlenote.common.profanity;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 비속어 검출 클라이언트의 기본 구현체
 */
@Component
public class DefaultProfanityClient implements ProfanityClient {

	private static final Logger log = LogManager.getLogger(DefaultProfanityClient.class);
	private final String PROFANITY_FILTER_URL;
	private final RestTemplate restTemplate;
	private final ProfanityFeginClient profanityFeginClient;

	public DefaultProfanityClient(
		@Value("${profanity.filter.url}") final String profanityFilterUrl,
		final RestTemplateBuilder restTemplateBuilder, ProfanityFeginClient profanityFeginClient
	) {
		this.PROFANITY_FILTER_URL = profanityFilterUrl;
		this.restTemplate = restTemplateBuilder.build();
		this.profanityFeginClient = profanityFeginClient;
	}

	@Override
	public boolean containsProfanity(String word) {
		// TODO 비속어 검출 로직 구현 필요

		log.info("검증 요청 : {}", word);

		long start = System.currentTimeMillis();  // 요청 시작 시간 측정

		ResponseEntity<ProfanityResult> response = profanityFeginClient.callProfanityFilter(word);

		long end = System.currentTimeMillis();  // 요청 종료 시간 측정

		log.info("검증 완료 : {}", response);
		log.info("응답 시간 : {} ms", end - start);  // 응답 시간 로그 출력

		return false;
	}

	@Override
	public ResponseEntity<ProfanityResult> newContainsProfanity(String word) {
		// TODO 비속어 검출 로직 구현 필요

		log.info("검증 요청 : {}", word);

		long start = System.currentTimeMillis();  // 요청 시작 시간 측정

		ResponseEntity<ProfanityResult> response = profanityFeginClient.callProfanityFilter(word);

		long end = System.currentTimeMillis();  // 요청 종료 시간 측정

		log.info("검증 완료 : {}", response);
		log.info("응답 시간 : {} ms", end - start);  // 응답 시간 로그 출력

		return response;
	}
}
