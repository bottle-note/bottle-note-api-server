package app.bottlenote.user.service;

import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.user.client.KakaoFeignClient;
import app.bottlenote.user.dto.response.KakaoTokenInfoResponse;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ThirdPartyService
@RequiredArgsConstructor
public class KakaoAuthService {

  private final KakaoFeignClient kakaoFeignClient;

  /** 우리 카카오 앱의 고유 ID. 타 앱에서 발급된 토큰을 걸러내는 데 쓴다. */
  @Value("${kakao.app-id}")
  private Long appId;

  @Transactional(readOnly = true)
  public KakaoUserResponse getUserInfo(String accessToken) {
    try {
      String authorization = "Bearer " + accessToken;

      verifyIssuedByOurApp(authorization);

      KakaoUserResponse kakaoUser = kakaoFeignClient.getUserInfo(authorization).getBody();

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
      log.warn("카카오 API Rate Limit 도달 - status: {}", e.status());
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    } catch (feign.FeignException e) {
      log.warn("카카오 API 호출 실패 - status: {}, message: {}", e.status(), e.getMessage());
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    } catch (Exception e) {
      log.error("카카오 API 예상치 못한 예외 - message: {}", e.getMessage(), e);
      throw new UserException(UserExceptionCode.KAKAO_API_ERROR);
    }
  }

  /**
   * 액세스 토큰이 우리 앱에서 발급된 것인지 확인한다.
   *
   * <p>카카오 토큰은 발급한 앱 안에서만 유효하지만, 타 앱에서 발급된 토큰도 그 앱 기준으로는 유효하므로 사용자 정보 조회 자체는 성공한다. 따라서 app_id를 대조하지
   * 않으면 제3자 앱의 토큰으로 우리 서비스에 로그인할 수 있다.
   */
  private void verifyIssuedByOurApp(String authorization) {
    KakaoTokenInfoResponse tokenInfo = kakaoFeignClient.getAccessTokenInfo(authorization).getBody();

    if (tokenInfo == null || !Objects.equals(tokenInfo.appId(), appId)) {
      log.warn(
          "카카오 app_id 불일치 - expected: {}, actual: {}",
          appId,
          tokenInfo != null ? tokenInfo.appId() : null);
      throw new UserException(UserExceptionCode.INVALID_KAKAO_ACCESS_TOKEN);
    }
  }
}
