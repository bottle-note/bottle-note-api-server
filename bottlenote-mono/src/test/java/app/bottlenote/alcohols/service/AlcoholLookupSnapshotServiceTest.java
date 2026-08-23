package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("AlcoholLookupSnapshotService 폴백 보호 테스트")
class AlcoholLookupSnapshotServiceTest {
  private InMemoryAlcoholQueryRepository alcoholQueryRepository;
  private InMemoryAlcoholLookupSnapshotStore snapshotStore;
  private AlcoholLookupSnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    snapshotStore = new InMemoryAlcoholLookupSnapshotStore();
    snapshotService = new AlcoholLookupSnapshotService(alcoholQueryRepository, snapshotStore);
  }

  @Test
  @DisplayName("version check가 실패하면 findAll 없이 stale cache를 즉시 사용한다")
  void findFilteredItems_whenVersionCheckFails_usesStaleCacheWithoutFindAll() {
    snapshotStore.replaceAll(List.of(snapshotItem(1L)));
    alcoholQueryRepository.save(createAlcohol(999L));
    ReflectionTestUtils.setField(snapshotService, "localCacheEnabled", true);
    ReflectionTestUtils.setField(snapshotService, "localCacheVersionCheckIntervalMs", 0L);

    snapshotService.findFilteredItems("macallan", null, null, null);
    int findAllCount = snapshotStore.findAllCount();
    snapshotStore.failVersionReads();

    List<AlcoholLookupItem> items = snapshotService.findFilteredItems("macallan", null, null, null);

    assertThat(items).extracting(AlcoholLookupItem::alcoholId).containsExactly(1L);
    assertThat(snapshotStore.findAllCount()).isEqualTo(findAllCount);
    assertThat(alcoholQueryRepository.findAllLookupItemsCount()).isZero();
  }

  @Test
  @DisplayName("Redis 조회가 실패하면 DB fallback 결과를 TTL 동안 재사용한다")
  void findFilteredItems_whenRedisFails_reusesDatabaseFallbackWithinTtl() {
    alcoholQueryRepository.save(createAlcohol(1L));
    snapshotStore.failReads();

    snapshotService.findFilteredItems("macallan", null, null, null);
    List<AlcoholLookupItem> second =
        snapshotService.findFilteredItems("macallan", null, null, null);

    assertThat(second).extracting(AlcoholLookupItem::alcoholId).containsExactly(1L);
    assertThat(alcoholQueryRepository.findAllLookupItemsCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("동시 Redis 실패는 한 번의 DB lookup으로 합친다")
  void findFilteredItems_whenConcurrentRedisFailures_singleFlightsDatabaseLookup()
      throws Exception {
    alcoholQueryRepository.save(createAlcohol(1L));
    alcoholQueryRepository.delayLookupItems(Duration.ofMillis(200));
    snapshotStore.failReads();
    snapshotService =
        new AlcoholLookupSnapshotService(
            alcoholQueryRepository,
            snapshotStore,
            false,
            1000L,
            // TTL을 두면 in-flight join을 놓친 늦은 도착도 캐시를 타므로, 스레드 순서와
            // 무관하게 DB 조회는 정확히 1회로 확정된다.
            new LookupDatabaseFallbackGuard(
                Clock.systemUTC(), Duration.ofSeconds(30), 3, Duration.ofSeconds(30)));
    ExecutorService pool = Executors.newFixedThreadPool(8);

    try {
      List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
      for (int i = 0; i < 8; i++) {
        futures.add(
            pool.submit(() -> snapshotService.findFilteredItems("macallan", null, null, null)));
      }
      for (java.util.concurrent.Future<?> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }
      assertThat(alcoholQueryRepository.findAllLookupItemsCount()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("DB fallback이 연속 실패하면 circuit를 열어 추가 DB 조회를 막는다")
  void findFilteredItems_whenDatabaseFallbackKeepsFailing_opensCircuit() {
    snapshotStore.failReads();
    alcoholQueryRepository.failLookupItems(new IllegalStateException("db down"));
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(Clock.systemUTC(), Duration.ZERO, 3, Duration.ofMinutes(5));
    snapshotService =
        new AlcoholLookupSnapshotService(
            alcoholQueryRepository, snapshotStore, false, 1000L, guard);

    // Redis와 DB가 모두 실패한 완전 장애는 빈 목록이 아니라 503으로 노출한다.
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> snapshotService.findFilteredItems("macallan", null, null, null))
          .isInstanceOf(AlcoholException.class)
          .hasMessage(AlcoholExceptionCode.ALCOHOL_LOOKUP_UNAVAILABLE.getMessage());
    }
    int calls = alcoholQueryRepository.findAllLookupItemsCount();
    assertThatThrownBy(() -> snapshotService.findFilteredItems("macallan", null, null, null))
        .isInstanceOf(AlcoholException.class);

    assertThat(calls).isEqualTo(3);
    assertThat(alcoholQueryRepository.findAllLookupItemsCount()).isEqualTo(3);
  }

  private static AlcoholLookupSnapshotItem snapshotItem(Long alcoholId) {
    return AlcoholLookupSnapshotItem.from(
        new AlcoholLookupItem(
            alcoholId,
            "맥캘란 " + alcoholId,
            "Macallan " + alcoholId,
            "싱글몰트",
            "Single Malt",
            SINGLE_MALT,
            1L,
            "스페이사이드",
            "Speyside",
            10L,
            "맥캘란",
            "Macallan",
            "https://example.com/alcohol.png"));
  }

  private static Alcohol createAlcohol(Long alcoholId) {
    Region region = Region.builder().korName("스페이사이드").engName("Speyside").build();
    ReflectionTestUtils.setField(region, "id", 1L);
    Distillery distillery = Distillery.builder().korName("맥캘란").engName("Macallan").build();
    ReflectionTestUtils.setField(distillery, "id", 10L);
    Alcohol alcohol =
        Alcohol.builder()
            .korName("맥캘란 " + alcoholId)
            .engName("Macallan " + alcoholId)
            .korCategory("싱글몰트")
            .engCategory("Single Malt")
            .categoryGroup(SINGLE_MALT)
            .type(app.bottlenote.alcohols.constant.AlcoholType.WHISKY)
            .region(region)
            .distillery(distillery)
            .imageUrl("https://example.com/alcohol.png")
            .build();
    ReflectionTestUtils.setField(alcohol, "id", alcoholId);
    return alcohol;
  }
}
