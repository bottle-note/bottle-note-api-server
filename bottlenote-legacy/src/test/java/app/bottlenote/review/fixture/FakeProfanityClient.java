package app.bottlenote.review.fixture;

import static app.bottlenote.shared.common.exception.CommonExceptionCode.CONTAINS_PROFANITY;

import app.bottlenote.common.profanity.FakeProfanityFeignClient;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.common.profanity.dto.request.ProfanityRequest;
import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import app.bottlenote.shared.common.exception.CommonException;
import org.springframework.http.ResponseEntity;

/** ProfanityClient 의 fake 구현체입니다. */
public class FakeProfanityClient implements ProfanityClient {

  FakeProfanityFeignClient fakeProfanityFeignClient = new FakeProfanityFeignClient();

  @Override
  public ProfanityResponse requestVerificationProfanity(String text) {
    var filter = ProfanityRequest.createFilter(text);
    ResponseEntity<ProfanityResponse> profanityResponseResponseEntity =
        fakeProfanityFeignClient.requestVerificationProfanity(filter);
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

  @Override
  public String filter(String content) {
    if (content == null || content.isBlank()) {
      return "";
    }
    return getFilteredText(content);
  }
}
