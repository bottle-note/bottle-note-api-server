package app.bottlenote.user.service;

import app.bottlenote.user.client.KakaoFeignClient;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import app.bottlenote.common.annotation.ThirdPartyService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ThirdPartyService
@RequiredArgsConstructor
public class KakaoAuthService {

  private final KakaoFeignClient kakaoFeignClient;

  @Transactional(readOnly = true)
  public KakaoUserResponse getUserInfo(String accessToken) {
    try {
      KakaoUserResponse kakaoUser = kakaoFeignClient.getUserInfo("Bearer " + accessToken).getBody();
      
      if (kakaoUser == null) {
        throw new UserException(UserExceptionCode.INVALID_KAKAO_ACCESS_TOKEN);
      }

      return kakaoUser;
      
    } catch (UserException e) {
      throw e;
    } catch (feign.FeignException.Unauthorized e) {
      log.error("카카오 토큰 검증 실패 (401): {}", e.getMessage());
      throw new UserException(UserExceptionCode.INVALID_KAKAO_ACCESS_TOKEN);
    } catch (Exception e) {
      log.error("카카오 API 호출 실패: {}", e.getMessage());
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    }
  }
}