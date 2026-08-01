package app.bottlenote.user.integration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import app.bottlenote.user.repository.OauthRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@DisplayName("[integration] 가입 상태 전환 잠금")
class SignupLockIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserTestFactory userFactory;
  @Autowired private OauthRepository oauthRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("소셜 로그인 잠금 중에는 가입 완료의 사용자 잠금이 대기한다")
  void social_login_and_signup_completion_share_user_lock() throws Exception {
    // given
    User user =
        userFactory.persistUser(
            User.builder()
                .status(UserStatus.SIGNUP_PENDING)
                .socialType(List.of(SocialType.KAKAO))
                .socialUniqueId("signup-lock-social-id"));
    CountDownLatch loginLocked = new CountDownLatch(1);
    CountDownLatch completeStarted = new CountDownLatch(1);
    CountDownLatch releaseLogin = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> login =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      ignored -> {
                        oauthRepository
                            .findBySocialUniqueIdForUpdate(user.getSocialUniqueId())
                            .orElseThrow();
                        loginLocked.countDown();
                        await(releaseLogin);
                      }));
      assertThat(loginLocked.await(5, SECONDS)).isTrue();

      Future<?> completion =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      ignored -> {
                        completeStarted.countDown();
                        oauthRepository.findByIdForUpdate(user.getId()).orElseThrow();
                      }));
      assertThat(completeStarted.await(5, SECONDS)).isTrue();

      // when & then
      try {
        assertThatThrownBy(() -> completion.get(300, MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
      } finally {
        releaseLogin.countDown();
      }
      login.get(5, SECONDS);
      completion.get(5, SECONDS);
    }
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
