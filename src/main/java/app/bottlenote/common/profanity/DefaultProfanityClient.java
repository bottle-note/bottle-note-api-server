package app.bottlenote.common.profanity;


import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.exception.CommonExceptionCode;
import app.bottlenote.common.profanity.fegin.ProfanityFeignClient;
import app.bottlenote.common.profanity.request.ProfanityRequest;
import app.bottlenote.common.profanity.response.ProfanityResponse;
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
	private final ProfanityFeignClient profanityFeignClient;

	public DefaultProfanityClient(ProfanityFeignClient profanityFeignClient) {
		this.profanityFeignClient = profanityFeignClient;
	}

	@Override
	public ProfanityResponse requestVerificationProfanity(String text) {
		log.info("[requestVerificationProfanity] 검증 요청 대상: {}", text);

		long start = System.currentTimeMillis();

		ProfanityRequest request = ProfanityRequest.createFilter(text);
		ResponseEntity<ProfanityResponse> response = profanityFeignClient.requestVerificationProfanity(request);
		var responseBody = response.getBody();

		long end = System.currentTimeMillis();

		log.info("응답 시간 : {} ms", end - start);  // 응답 시간 로그 출력

		log.info("검증 완료 : [ Code: {}] ,[Header: {}]  ", response.getStatusCode(), response.getHeaders());

		return responseBody;
	}

	@Override
	public String getFilteredText(String text) {
		log.info("[getFilteredText] 필터링 요청 대상: {}", text);
		ProfanityResponse response = requestVerificationProfanity(text);
		if (Boolean.TRUE.equals(response.isNotFiltered())) {
			return text;
		}
		return response.filtered();
	}

	@Override
	public void validateProfanity(String text) {
		log.info("[validateProfanity] 검증 요청 대상: {}", text);
		ProfanityResponse response = requestVerificationProfanity(text);
		if (Boolean.TRUE.equals(response.isProfane())) {
			throw new CommonException(CommonExceptionCode.CONTAINS_PROFANITY);
		}
	}
}
