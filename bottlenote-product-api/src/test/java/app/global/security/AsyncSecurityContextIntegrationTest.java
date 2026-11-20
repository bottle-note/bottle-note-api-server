package app.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("integration")
@DisplayName("[integration] 비동기 작업의 인증 전달")
class AsyncSecurityContextIntegrationTest extends IntegrationTestSupport {

  @Autowired
  @Qualifier("applicationTaskExecutor")
  private ThreadPoolTaskExecutor applicationTaskExecutor;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("새 스레드를 만들 때 요청 스레드의 인증을 물려주지 않는다")
  void 새_스레드는_요청_스레드의_인증을_물려받지_않는다() throws InterruptedException {
    // given
    authenticate(new TestingAuthenticationToken("admin", "credentials", "ROLE_ADMIN"));
    AtomicReference<Authentication> seenByChild = new AtomicReference<>();

    // when
    Thread child =
        new Thread(() -> seenByChild.set(SecurityContextHolder.getContext().getAuthentication()));
    child.start();
    child.join();

    // then
    assertThat(seenByChild.get()).isNull();
  }

  @Test
  @DisplayName("비동기 작업을 실행할 때 제출 시점의 인증만 받고 재사용된 워커에 이전 인증이 남지 않는다")
  void 비동기_작업은_제출_시점의_인증만_받는다() throws Exception {
    // 코어 수의 두 배를 제출해 모든 워커가 관리자 인증 중에 만들어지고 재사용되게 한다
    int taskCount = applicationTaskExecutor.getCorePoolSize() * 2;
    Authentication admin = new TestingAuthenticationToken("admin", "credentials", "ROLE_ADMIN");
    Authentication user = new TestingAuthenticationToken("user", "credentials", "ROLE_USER");

    // given, when: 관리자 인증으로 제출
    authenticate(admin);
    List<Authentication> seenWithAdmin = submitAndCollect(taskCount);

    // given, when: 인증 없이 제출
    SecurityContextHolder.clearContext();
    List<Authentication> seenWithoutAuth = submitAndCollect(taskCount);

    // given, when: 다른 사용자 인증으로 제출
    authenticate(user);
    List<Authentication> seenWithUser = submitAndCollect(taskCount);

    // then
    assertThat(seenWithAdmin).hasSize(taskCount).allMatch(seen -> seen == admin);
    assertThat(seenWithoutAuth).hasSize(taskCount).allMatch(seen -> seen == null);
    assertThat(seenWithUser).hasSize(taskCount).allMatch(seen -> seen == user);
  }

  private List<Authentication> submitAndCollect(int taskCount) throws Exception {
    List<Future<Authentication>> futures = new ArrayList<>();
    for (int i = 0; i < taskCount; i++) {
      futures.add(
          applicationTaskExecutor.submit(
              () -> SecurityContextHolder.getContext().getAuthentication()));
    }
    List<Authentication> seen = new ArrayList<>();
    for (Future<Authentication> future : futures) {
      seen.add(future.get(10, TimeUnit.SECONDS));
    }
    return seen;
  }

  private static void authenticate(Authentication authentication) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
