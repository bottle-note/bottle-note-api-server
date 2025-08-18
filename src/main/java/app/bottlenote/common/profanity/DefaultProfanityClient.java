package app.bottlenote.common.profanity;

import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.exception.CommonExceptionCode;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** 비속어 검출 클라이언트의 기본 구현체 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultProfanityClient implements ProfanityClient {
  private final ProfanityFeignClient profanityFeignClient;

  @Override
  public ProfanityResponse requestVerificationProfanity(String text) {
    log.info("[requestVerificationProfanity] 검증 요청 대상: {}", text);
    long start = System.currentTimeMillis();
    ProfanityRequest request = ProfanityRequest.createFilter(text);
    ResponseEntity<ProfanityResponse> response =
        profanityFeignClient.requestVerificationProfanity(request);
    var responseBody = response.getBody();
    long end = System.currentTimeMillis();

    log.info(
        "검증 완료 : [ Code: {}] ,[Header: {}] 응답 시간 : {} ms\"  ",
        Objects.requireNonNull(responseBody).status().code(),
        responseBody.status().message(),
        end - start);
    return responseBody;
  }

  @Override
  public String getFilteredText(String text) {
    log.info("[getFilteredText] 필터링 요청 대상: {}", text);
    ProfanityResponse response = requestVerificationProfanity(text);
    if (response.isNotFiltered()) {
      return text;
    }
    return response.filtered();
  }

  @Override
  public String filter(String content) {
    if (content == null || content.isBlank()) {
      return "";
    }
    return getFilteredText(content);
  }

  @Override
  public void validateProfanity(String text) {
    log.info("[validateProfanity] 검증 요청 대상: {}", text);
    ProfanityResponse response = requestVerificationProfanity(text);
    if (response.isProfane()) {
      throw new CommonException(CommonExceptionCode.CONTAINS_PROFANITY);
    }
  }
}
