package app.bottlenote.user.service;

import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.user.client.KakaoFeignClient;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.warn(
            "카카오 API 응답 null - accessToken 길이: {}", accessToken != null ? accessToken.length() : 0);
        throw new UserException(UserExceptionCode.INVALID_KAKAO_ACCESS_TOKEN);
      }

      return kakaoUser;

    } catch (UserException e) {
      throw e;
    } catch (feign.FeignException.Unauthorized e) {
      log.warn("카카오 토큰 검증 실패 (401) - status: {}, message: {}", e.status(), e.getMessage());
      throw new UserException(UserExceptionCode.INVALID_KAKAO_ACCESS_TOKEN);
    } catch (feign.FeignException.TooManyRequests e) {
      log.warn("카카오 API Rate Limit 도달 - status: {}, retryAfter: {}초", e.status(), e.retryAfter());
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    } catch (feign.FeignException e) {
      log.warn("카카오 API 호출 실패 - status: {}, message: {}", e.status(), e.getMessage());
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    } catch (Exception e) {
      log.error("카카오 API 예상치 못한 예외 - message: {}", e.getMessage(), e);
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    }
  }
}
