package app.bottlenote.user.fake;

import app.bottlenote.user.client.KakaoFeignClient;
import app.bottlenote.user.dto.response.KakaoUserResponse;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class FakeKakaoFeignClient implements KakaoFeignClient {

  private final Map<String, KakaoUserResponse> tokenDatabase = new HashMap<>();
  private boolean simulateUnauthorized = false;
  private boolean simulateServerError = false;

  public FakeKakaoFeignClient() {
    setupDefaultUsers();
  }

  private void setupDefaultUsers() {
    // 기본 테스트 사용자 1 - 모든 정보 포함
    KakaoUserResponse.Profile profile1 =
        new KakaoUserResponse.Profile(
            "홍길동",
            "http://test.kakao.com/thumbnail.jpg",
            "http://test.kakao.com/profile.jpg",
            false);

    KakaoUserResponse.KakaoAccount account1 =
        new KakaoUserResponse.KakaoAccount(
            false,
            profile1,
            false,
            "홍길동",
            false,
            "test@kakao.com",
            true,
            true,
            false,
            "20~29",
            false,
            "female");

    KakaoUserResponse user1 =
        new KakaoUserResponse(123456789L, LocalDateTime.of(2022, 4, 11, 1, 45, 28), account1);

    tokenDatabase.put("Bearer valid_token_with_email", user1);

    // 기본 테스트 사용자 2 - 이메일 없음
    KakaoUserResponse.Profile profile2 = new KakaoUserResponse.Profile("김테스트", null, null, true);

    KakaoUserResponse.KakaoAccount account2 =
        new KakaoUserResponse.KakaoAccount(
            false, profile2, false, null, true, null, null, null, false, "30~39", false, "male");

    KakaoUserResponse user2 =
        new KakaoUserResponse(987654321L, LocalDateTime.of(2022, 5, 15, 10, 30, 0), account2);

    tokenDatabase.put("Bearer valid_token_without_email", user2);

    // 기존 회원 테스트용
    KakaoUserResponse.KakaoAccount account3 =
        new KakaoUserResponse.KakaoAccount(
            false,
            null,
            false,
            null,
            false,
            "existing@test.com",
            true,
            true,
            false,
            "40~49",
            false,
            "female");

    KakaoUserResponse user3 = new KakaoUserResponse(555555555L, LocalDateTime.now(), account3);

    tokenDatabase.put("Bearer existing_user_token", user3);
  }

  @Override
  public ResponseEntity<KakaoUserResponse> getUserInfo(String authorization) {
    // log.info("[Fake] 카카오 사용자 정보 조회 요청: {}", authorization);

    if (simulateUnauthorized) {
      throw new FeignException.Unauthorized(
          "Unauthorized",
          feign.Request.create(feign.Request.HttpMethod.GET, "test", Map.of(), null, null, null),
          null,
          Map.of());
    }

    if (simulateServerError) {
      throw new FeignException.InternalServerError(
          "Server Error",
          feign.Request.create(feign.Request.HttpMethod.GET, "test", Map.of(), null, null, null),
          null,
          Map.of());
    }

    KakaoUserResponse user = tokenDatabase.get(authorization);
    if (user == null) {
      throw new FeignException.Unauthorized(
          "Invalid token",
          feign.Request.create(feign.Request.HttpMethod.GET, "test", Map.of(), null, null, null),
          null,
          Map.of());
    }

    return ResponseEntity.ok(user);
  }

  // 테스트 헬퍼 메서드들
  public void addTestUser(String token, KakaoUserResponse user) {
    tokenDatabase.put("Bearer " + token, user);
  }

  public void simulateUnauthorizedError() {
    this.simulateUnauthorized = true;
  }

  public void simulateServerError() {
    this.simulateServerError = true;
  }

  public void resetSimulation() {
    this.simulateUnauthorized = false;
    this.simulateServerError = false;
  }

  public void clear() {
    tokenDatabase.clear();
    resetSimulation();
    setupDefaultUsers();
  }
}
