package app.bottlenote.curation.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.curation.domain.CurationSpecRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * product의 모든 레플리카가 기동하면서 큐레이션 스펙 동기화를 동시에 실행하는 상황을 재현한다.
 *
 * <p>빈 테이블에서 여러 스레드가 같은 code를 함께 insert하면 uk_curation_spec_code 충돌이나 잠금 충돌이 난다. 충돌한 쪽은 다시 시도해 이미
 * 저장된 행을 update하는 경로로 수렴해야 하며, 어느 스레드도 실패하지 않아야 한다.
 */
@Tag("integration")
@DisplayName("[integration] 큐레이션 스펙 동기화 동시 실행")
class CurationSpecResourceSyncRunnerConcurrencyIntegrationTest extends IntegrationTestSupport {

  private static final int REPLICAS = 6;

  @Autowired private CurationSpecResourceSyncRunner runner;
  @Autowired private CurationSpecRepository curationSpecRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("여러 인스턴스가 빈 테이블에서 동시에 동기화할 때 모두 성공하고 스펙은 code마다 하나만 남는다")
  void 동시_기동_동기화는_실패하지_않는다() throws Exception {
    // 컨텍스트 기동 시 이미 한 번 동기화했으므로 비운 상태에서 시작한다
    dataInitializer.deleteAll();
    assertThat(countSpecs()).isZero();

    ExecutorService executor = Executors.newFixedThreadPool(REPLICAS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> results = new ArrayList<>();
    try {
      for (int i = 0; i < REPLICAS; i++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  runner.sync();
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> result : results) {
        // 한 인스턴스라도 예외로 끝나면 get()이 그 예외를 던진다
        result.get(60, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    List<String> codes =
        jdbcTemplate.queryForList("SELECT code FROM curation_spec ORDER BY code", String.class);
    assertThat(codes)
        .containsExactly("PROGRAM", "RECOMMENDED_WHISKY", "WHISKY_PAIRING", "WHISKY_TASTING_EVENT");
    assertThat(curationSpecRepository.findByCode("RECOMMENDED_WHISKY")).isPresent();
  }

  private int countSpecs() {
    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM curation_spec", Integer.class);
    return count == null ? 0 : count;
  }
}
