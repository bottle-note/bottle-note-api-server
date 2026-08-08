package app.bottlenote.accesscontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.domain.IpBanEventRepository;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@DisplayName("[integration] IP 차단 동시성")
@ActiveProfiles({"test", "access-control-it"})
class IpBanConcurrencyIntegrationTest extends IntegrationTestSupport {

  @Autowired private IpBanFacade ipBanFacade;
  @Autowired private IpBanEventRepository ipBanEventRepository;

  @Test
  @DisplayName("동일 IP 동시 ban은 하나의 현재 상태와 BAN-EXTEND 감사 순서로 직렬화한다")
  void ban_whenSameIpConcurrent_serializesCurrentStateAndEvents() throws Exception {
    String ip = "198.18.9." + (Math.floorMod(UUID.randomUUID().hashCode(), 250) + 1);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = executor.submit(() -> banAfterStart(ip, "first", ready, start));
      Future<?> second = executor.submit(() -> banAfterStart(ip, "second", ready, start));

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);

      IpBanDetailResponse detail = ipBanFacade.findByIp(ip).orElseThrow();
      List<IpBanEventType> eventTypes =
          ipBanEventRepository.findByIpBanIdOrderByIdAsc(detail.id()).stream()
              .map(event -> event.getEventType())
              .toList();

      assertThat(detail.status()).isEqualTo(IpBanStatus.ACTIVE);
      assertThat(eventTypes).containsExactly(IpBanEventType.BAN, IpBanEventType.EXTEND);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  private void banAfterStart(String ip, String reason, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 시작 신호를 받지 못했습니다.");
      }
      ipBanFacade.ban(ip, Duration.ofMinutes(10), reason, null);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
